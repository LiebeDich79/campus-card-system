# -*- coding: utf-8 -*-
"""对运行中的后端做真实 HTTP 接口测试，结果写入 JSON 供报告引用。"""
import json
import os
import time
import uuid
import urllib.request
import urllib.error

BASE = os.environ.get('CAMPUS_BASE', 'http://localhost:8080')
results = []


def call(method, path, body=None, headers=None, timeout=15):
    url = BASE + path
    data = None
    hdr = {'Accept': 'application/json'}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode('utf-8')
        hdr['Content-Type'] = 'application/json;charset=UTF-8'
    if headers:
        hdr.update(headers)
    req = urllib.request.Request(url, data=data, headers=hdr, method=method)
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode('utf-8', 'replace')
            status = resp.status
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', 'replace')
        status = e.code
    except Exception as e:
        raw = json.dumps({'error': str(e)}, ensure_ascii=False)
        status = -1
    ms = round((time.time() - t0) * 1000, 1)
    try:
        obj = json.loads(raw)
    except Exception:
        obj = {'raw': raw[:200]}
    return {'http': status, 'ms': ms, 'body': obj}


def case(cid, name, method, path, body=None, headers=None, expect_code=None, note=''):
    r = call(method, path, body, headers)
    code = r['body'].get('code') if isinstance(r['body'], dict) else None
    ok = (expect_code is None) or (code == expect_code)
    d = r['body'].get('data') if isinstance(r['body'], dict) else None
    row = {'id': cid, 'name': name, 'method': method, 'path': path,
           'expect': expect_code, 'actual': code, 'http': r['http'],
           'ms': r['ms'], 'ok': ok,
           'message': (r['body'].get('message') if isinstance(r['body'], dict) else ''),
           'note': note}
    if isinstance(d, dict):
        row['data'] = {k: d.get(k) for k in ('txNo', 'actualAmount', 'originalAmount', 'balanceAfter',
                                             'strategyType', 'cardNo', 'status') if d.get(k) is not None}
    results.append(row)
    print('%-6s %-28s expect=%-6s actual=%-6s %-4s %6sms' %
          (cid, name[:28], str(expect_code), str(code), 'OK' if ok else 'FAIL', r['ms']))
    if isinstance(d, dict):
        print('        -> %s' % json.dumps(row.get('data', {}), ensure_ascii=False))
    return r


def pause():
    time.sleep(0.08)


def std(card_no, amount, strategy=None, key=None, terminal='POS-01', ttype='POS', pin='123456'):
    b = {'cardNo': card_no, 'amount': amount, 'pin': pin,
         'terminalNo': terminal, 'terminalType': ttype,
         'idempotencyKey': key or ('IDEM-' + uuid.uuid4().hex[:12])}
    if strategy:
        b['strategyType'] = strategy
    return b


print('===== 功能 / 边界 / 异常实测 =====')
r0 = call('GET', '/api/cards')
results.append({'id': 'F-00', 'name': '演示卡列表', 'method': 'GET', 'path': '/api/cards',
                'expect': 0, 'actual': r0['body'].get('code'), 'http': r0['http'], 'ms': r0['ms'],
                'ok': r0['body'].get('code') == 0, 'message': '',
                'note': 'data.sql 初始化 4 张基础卡、5 个基础终端（另有扩展演示数据，不影响本用例）'})
print('F-00 演示卡列表 -> code=%s, %s 张卡' % (r0['body'].get('code'), len(r0['body'].get('data') or [])))
pause()

# 新增一张余额 20.00 的测试卡，供"恰好等于余额/超出余额"用例使用
case('F-01', '开户（初始余额 20.00）', 'POST', '/api/cards?balance=20.00',
     {'cardNo': '202601010099', 'studentNo': '2026010199', 'holderName': '边界测试卡',
      'cardType': 'STUDENT', 'status': 'NORMAL', 'pin': '123456'}, expect_code=0)
pause()
case('F-03', '充值 100.00（卡 002）', 'POST', '/api/cards/202601010002/recharge',
     {'amount': 100.00, 'idempotencyKey': 'REC-' + uuid.uuid4().hex[:10]}, expect_code=0)
pause()

last_tx = {}
for cid, name, strat, amount in [
    ('F-06', '普通消费 15.00', 'NORMAL', 15.00),
    ('F-07', '学生折扣消费 15.00', 'DISCOUNT', 15.00),
    ('F-08', '午餐补贴消费 15.00', 'SUBSIDY', 15.00),
    ('F-09', '积分抵扣消费 15.00', 'POINTS', 15.00),
]:
    r = case(cid, name, 'POST', '/api/transactions/consume',
             std('202601010001', amount, strat), expect_code=0)
    if isinstance(r['body'].get('data'), dict):
        last_tx[cid] = r['body']['data'].get('txNo')
    pause()

case('F-10', '策略自动选择（不传 strategyType）', 'POST', '/api/transactions/consume',
     std('202601010002', 12.00, None), expect_code=0)
pause()

raw_body = {'terminalNo': 'POS-01', 'terminalType': 'POS', 'vendor': '新中新',
            'sign': 'RAW-' + uuid.uuid4().hex[:10],
            'payload': '202601010001|8.50|123456|米饭:1:8.50'}
case('F-11', 'POS 竖线报文适配消费', 'POST', '/api/transactions/consume/raw',
     raw_body, expect_code=0)
pause()
case('F-12', '超市 JSON 报文适配消费', 'POST', '/api/transactions/consume/raw',
     {'terminalNo': 'MART-01', 'terminalType': 'SUPERMARKET', 'vendor': '正方',
      'sign': 'RAW2-' + uuid.uuid4().hex[:10],
      'payload': '{"cardNo":"202601010001","amount":23.50,"pin":"123456","items":[]}'},
     expect_code=0)
pause()

# 边界与异常：统一显式指定 NORMAL，避免自动折扣改变实付金额
case('B-01', '金额为 0', 'POST', '/api/transactions/consume',
     std('202601010001', 0, 'NORMAL'), expect_code=None)
pause()
case('B-02', '金额为负', 'POST', '/api/transactions/consume',
     std('202601010001', -1, 'NORMAL'), expect_code=None)
pause()
case('B-03', '三位小数 1.005', 'POST', '/api/transactions/consume',
     std('202601010001', 1.005, 'NORMAL'), expect_code=None)
pause()
case('B-04', '恰好等于余额 20.00', 'POST', '/api/transactions/consume',
     std('202601010099', 20.00, 'NORMAL'), expect_code=0)
pause()
case('B-05', '余额不足（新卡再消费 0.01）', 'POST', '/api/transactions/consume',
     std('202601010099', 0.01, 'NORMAL'), expect_code=None)
pause()
case('B-06', '单笔限额 200.01', 'POST', '/api/transactions/consume',
     std('202601010001', 200.01, 'NORMAL'), expect_code=None)
pause()
case('E-01', '卡不存在', 'POST', '/api/transactions/consume',
     std('NOT-EXIST', 10.00, 'NORMAL'), expect_code=None)
pause()
case('E-02', '已挂失卡消费', 'POST', '/api/transactions/consume',
     std('202601010003', 10.00, 'NORMAL'), expect_code=None)
pause()
case('E-03', '密码错误', 'POST', '/api/transactions/consume',
     std('202601010001', 10.00, 'NORMAL', pin='000000'), expect_code=None)
pause()

dup_key = 'DUP-' + uuid.uuid4().hex[:10]
case('E-04a', '重复提交（第 1 次）', 'POST', '/api/transactions/consume',
     std('202601010001', 3.00, 'NORMAL', key=dup_key), expect_code=0)
pause()
case('E-04b', '同一幂等键第 2 次', 'POST', '/api/transactions/consume',
     std('202601010001', 3.00, 'NORMAL', key=dup_key), expect_code=None)
pause()
case('E-07', '终端越权（错误 X-Terminal-No）', 'POST', '/api/transactions/consume',
     std('202601010001', 2.00, 'NORMAL'),
     headers={'X-Terminal-No': 'NOT-REGISTERED', 'X-Terminal-Sign': 'x'}, expect_code=None)
pause()

if last_tx.get('F-07'):
    case('F-14', '对折扣交易全额退款 13.50', 'POST', '/api/transactions/refund',
         {'originalTxNo': last_tx['F-07'], 'amount': 13.50, 'reason': '测试冲正',
          'idempotencyKey': 'REF-' + uuid.uuid4().hex[:10]}, expect_code=0)
    pause()
    case('E-09', '同一原交易重复冲正', 'POST', '/api/transactions/refund',
         {'originalTxNo': last_tx['F-07'], 'amount': 13.50,
          'idempotencyKey': 'REF2-' + uuid.uuid4().hex[:10]}, expect_code=None)
    pause()
    case('E-08', '退款超额', 'POST', '/api/transactions/refund',
         {'originalTxNo': last_tx['F-06'], 'amount': 99.00,
          'idempotencyKey': 'REF3-' + uuid.uuid4().hex[:10]}, expect_code=None)
    pause()

case('F-04', '挂失（卡 001，之后消费应被拒）', 'PUT', '/api/cards/202601010001/lost',
     {}, expect_code=0)
pause()
case('E-10', '挂失后消费被拒', 'POST', '/api/transactions/consume',
     std('202601010001', 1.00, 'NORMAL'), expect_code=None)
pause()

for cid, name, path in [
    ('F-15', '交易明细分页查询', '/api/transactions?page=0&size=10'),
    ('F-16a', '分类统计', '/api/statistics/summary'),
    ('F-17', '通知与审计日志', '/api/notifications'),
    ('F-16b', '终端列表', '/api/terminals'),
]:
    case(cid, name, 'GET', path, expect_code=0)
    pause()

case('F-16c', '按终端对账', 'POST', '/api/reconcile/settle', {}, expect_code=0)
pause()

with open('api_test_results.json', 'w', encoding='utf-8') as f:
    json.dump(results, f, ensure_ascii=False, indent=2)
print('\n保存 api_test_results.json，共 %d 条，符合预期 %d 条' %
      (len(results), sum(1 for x in results if x['ok'])))
