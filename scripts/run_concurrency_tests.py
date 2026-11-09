# -*- coding: utf-8 -*-
"""真实 HTTP 并发压测：同一幂等键竞争、不同键并发扣款、混合查询负载。"""
import json
import os
import time
import uuid
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor

BASE = os.environ.get('CAMPUS_BASE', 'http://localhost:8080')
CARD = 'T20260001'
PIN = '888888'


def post(path, body):
    data = json.dumps(body, ensure_ascii=False).encode('utf-8')
    req = urllib.request.Request(BASE + path, data=data,
                                 headers={'Content-Type': 'application/json;charset=UTF-8',
                                          'Accept': 'application/json'}, method='POST')
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            obj = json.loads(resp.read().decode('utf-8', 'replace'))
            status = resp.status
    except urllib.error.HTTPError as e:
        obj = json.loads(e.read().decode('utf-8', 'replace'))
        status = e.code
    except Exception as e:
        obj = {'code': -1, 'message': str(e)}
        status = -1
    return {'code': obj.get('code'), 'msg': obj.get('message'), 'http': status,
            'ms': (time.time() - t0) * 1000}


def get(path):
    req = urllib.request.Request(BASE + path, headers={'Accept': 'application/json'})
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            obj = json.loads(resp.read().decode('utf-8', 'replace'))
            status = resp.status
    except Exception as e:
        obj = {'code': -1}
        status = -1
    return {'code': obj.get('code'), 'http': status, 'ms': (time.time() - t0) * 1000}


def consume(key, amount=1.00):
    return post('/api/transactions/consume', {
        'cardNo': CARD, 'amount': amount, 'pin': PIN,
        'terminalNo': 'POS-01', 'terminalType': 'POS',
        'strategyType': 'NORMAL', 'idempotencyKey': key})


def stat(rs):
    ms = sorted(x['ms'] for x in rs)
    n = len(ms)
    return {'n': n,
            'avg_ms': round(sum(ms) / n, 1),
            'p95_ms': round(ms[int(n * 0.95) - 1], 1) if n >= 20 else round(ms[-1], 1),
            'max_ms': round(ms[-1], 1),
            'min_ms': round(ms[0], 1)}


def codes(rs):
    d = {}
    for x in rs:
        d[str(x['code'])] = d.get(str(x['code']), 0) + 1
    return d


out = {}

# P-01 同一幂等键 100 并发
print('P-01 同一幂等键 100 线程竞争 ...')
same_key = 'CONC-SAME-' + uuid.uuid4().hex[:10]
t0 = time.time()
with ThreadPoolExecutor(max_workers=100) as ex:
    rs = list(ex.map(lambda i: consume(same_key), range(100)))
wall = round((time.time() - t0) * 1000, 1)
out['P-01'] = {'desc': '同一幂等键 100 线程并发消费 1.00 元',
               'codes': codes(rs), 'latency': stat(rs), 'wall_ms': wall}
print('   ', json.dumps(out['P-01'], ensure_ascii=False))

time.sleep(1.2)

# P-02 不同幂等键 100 并发扣款
print('P-02 100 个不同幂等键并发扣款 ...')
keys = ['CONC-DIFF-%s-%03d' % (uuid.uuid4().hex[:6], i) for i in range(100)]
t0 = time.time()
with ThreadPoolExecutor(max_workers=100) as ex:
    rs2 = list(ex.map(lambda k: consume(k), keys))
wall2 = round((time.time() - t0) * 1000, 1)
out['P-02'] = {'desc': '100 个不同幂等键并发，每笔 1.00 元',
               'codes': codes(rs2), 'latency': stat(rs2), 'wall_ms': wall2}
print('   ', json.dumps(out['P-02'], ensure_ascii=False))

time.sleep(1.2)

# P-03 混合读写 200 请求（50 并发 × 4 轮）
print('P-03 混合读写 200 请求 ...')
t0 = time.time()
with ThreadPoolExecutor(max_workers=50) as ex:
    futs = []
    for i in range(200):
        if i % 4 == 0:
            futs.append(ex.submit(consume, 'MIX-%s-%03d' % (uuid.uuid4().hex[:6], i)))
        else:
            futs.append(ex.submit(get, '/api/transactions?page=0&size=10'))
    rs3 = [f.result() for f in futs]
wall3 = round((time.time() - t0) * 1000, 1)
errs = sum(1 for x in rs3 if x['http'] != 200)
out['P-03'] = {'desc': '50 并发混合负载 200 请求（1/4 消费 + 3/4 明细分页查询）',
               'http_non_200': errs, 'codes': codes(rs3), 'latency': stat(rs3),
               'wall_ms': wall3, 'qps': round(200 / (wall3 / 1000.0), 1)}
print('   ', json.dumps(out['P-03'], ensure_ascii=False))

with open('concurrency_results.json', 'w', encoding='utf-8') as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
print('\n保存 concurrency_results.json')
