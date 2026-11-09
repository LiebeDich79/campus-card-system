# -*- coding: utf-8 -*-
"""演示数据自检：通过 REST 接口确认各页面有数据可看，且对账金额自洽。

用法（后端已启动后执行）：
    python scripts/check_demo_data.py            # 默认 8080
    python scripts/check_demo_data.py 18080     # 指定端口

只读取接口，不写入任何数据；可随时重复运行。
账户余额与流水、积分、对账的账实相符校验在 SQL 层完成（见 docs/数据库设计.md）。
"""
import json
import sys
import urllib.parse
import urllib.request
from datetime import date, timedelta

try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

PORT = sys.argv[1] if len(sys.argv) > 1 else '8080'
BASE = 'http://localhost:%s/api' % PORT
TODAY = date.today()
WINDOW = {'startDate': (TODAY - timedelta(days=30)).isoformat(), 'endDate': TODAY.isoformat()}


def call(path, params=None, method='GET'):
    url = BASE + path + ('?' + urllib.parse.urlencode(params) if params else '')
    req = urllib.request.Request(url, data=b'' if method == 'POST' else None, method=method)
    with urllib.request.urlopen(req, timeout=10) as resp:
        body = json.loads(resp.read().decode('utf-8'))
    if body.get('code') != 0:
        raise RuntimeError('%s -> %s' % (path, body.get('message')))
    return body.get('data')


def num(v):
    return float(v or 0)


checks = []


def check(name, ok, detail):
    checks.append((name, bool(ok), detail))
    print('%-38s %s  %s' % (name, 'PASS' if ok else 'FAIL', detail))


cards = call('/cards')
terminals = call('/terminals')
tx = call('/transactions', {'page': 1, 'size': 200})
summary = call('/statistics/summary', WINDOW)
notices = call('/notifications')
records = call('/reconcile/records')
settled = call('/reconcile/settle', WINDOW, 'POST')

print('接口地址 %s，统计窗口 %s ~ %s' % (BASE, WINDOW['startDate'], WINDOW['endDate']))
print('-' * 72)
check('卡片列表（含 6 张扩展卡）', len(cards) >= 10, '%d 张' % len(cards))
check('终端列表（含 3 个扩展终端）', len(terminals) >= 8, '%d 个' % len(terminals))
check('交易流水（含 28 笔历史）', tx.get('total', 0) >= 28, 'total=%s，本页 %d 条' % (tx.get('total'), len(tx.get('list', []))))

rows = tx.get('list', [])
types = set(r.get('txType') for r in rows)
strategies = set(r.get('strategyType') for r in rows if r.get('txType') == 'CONSUME')
statuses = set(r.get('status') for r in rows)
check('交易类型覆盖', {'RECHARGE', 'CONSUME', 'REFUND'} <= types, ', '.join(sorted(t for t in types if t)))
check('优惠策略覆盖', {'NORMAL', 'DISCOUNT', 'SUBSIDY', 'POINTS'} <= strategies,
      ', '.join(sorted(s for s in strategies if s)))
check('异常状态留痕', {'REVERSED', 'FAILED'} <= statuses, ', '.join(sorted(s for s in statuses if s)))

print('-' * 72)
print('统计页：交易笔数 %s，消费 %s，充值 %s，退款 %s，净消费 %s，终端类别 %d 组'
      % (summary.get('txCount'), summary.get('consumeTotal'), summary.get('rechargeTotal'),
         summary.get('refundTotal'), summary.get('netAmount'), len(summary.get('groupByTerminal', []))))
check('统计页有汇总数据', num(summary.get('consumeTotal')) >= 400 and len(summary.get('groupByTerminal', [])) >= 3,
      '消费合计 %s' % summary.get('consumeTotal'))
channels = set(n.get('channel') for n in notices)
check('通知页有 PUSH / AUDIT', {'PUSH', 'AUDIT'} <= channels, '%d 条，渠道 %s'
      % (len(notices), ', '.join(sorted(c for c in channels if c))))
check('历史对账记录', len(records) >= 23, '%d 条' % len(records))

sum_records = round(sum(num(r.get('total_amount', r.get('totalAmount'))) for r in records), 2)
sum_settled = round(sum(num(r.get('total_amount', r.get('totalAmount'))) for r in settled), 2)
check('对账单与历史记录金额一致', abs(sum_records - sum_settled) < 0.01 and len(settled) >= 20,
      '记录 %s 元 / 实时汇总 %s 元（%d 组）' % (sum_records, sum_settled, len(settled)))

ext_no = {'202601010004', '202601010005', '202601010006', '202601010007', 'T20260002', 'T20260003'}
ext = [c for c in cards if c.get('cardNo') in ext_no]
ext_status = set(c.get('status') for c in ext)
check('扩展卡状态覆盖', len(ext) >= 6 and {'NORMAL', 'FROZEN', 'CANCELLED', 'LOST'} <= ext_status,
      '%d 张，状态 %s' % (len(ext), ', '.join(sorted(s for s in ext_status if s))))

print('-' * 72)
failed = [n for n, ok, _ in checks if not ok]
print('结果：%d/%d 项通过' % (len(checks) - len(failed), len(checks)))
if failed:
    print('未通过：' + '、'.join(failed))
sys.exit(1 if failed else 0)
