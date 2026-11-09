# -*- coding: utf-8 -*-
"""生成扩展测试数据，并同步注入 sql/init.sql 与 src/main/resources/data.sql。

脚本可放在 _build/ 或工程的 scripts/ 下运行，会自动定位工程根目录；
两份 SQL 中的生成段带 BEGIN/END 标记，重复运行只会替换该段，不会重复追加。

设计原则：
1. 全部 INSERT IGNORE + 显式大 id（>=100），重复启动不覆盖已有数据、不与运行时自增 id 冲突；
2. 只给新增卡片挂历史流水，原有 4 张演示卡的初始余额保持不变；
3. account.balance / points 由流水按业务规则反推，reconcile_record 按 settle 接口同一口径汇总；
4. created_at 使用相对日期 DATE_SUB(CURDATE(), INTERVAL n DAY)，保证任何时间部署都落在"近 30 日"统计窗口内。
"""
import json
import os
import re
from decimal import Decimal

HERE = os.path.dirname(os.path.abspath(__file__))
IN_BUILD = os.path.basename(HERE) == '_build'
BASE = os.path.dirname(HERE)
PROJ = os.path.join(BASE, '期末考查提交物', 'campus-card') if IN_BUILD else BASE
INIT = os.path.join(PROJ, 'sql', 'init.sql')
DATA = os.path.join(PROJ, 'src', 'main', 'resources', 'data.sql')

BEGIN = '-- ===== BEGIN GENERATED TEST DATA (add_test_data.py) ====='
END = '-- ===== END GENERATED TEST DATA (add_test_data.py) ====='

D = lambda s: Decimal(str(s))
f2 = lambda d: '%.2f' % d

# ---------------------------------------------------------------- 基础数据
# id, 卡号, 学号/工号, 姓名, 类型, 状态, 密码, 期初余额, 期初积分
CARDS = [
    (100, '202601010004', '2026010104', '陈同学', 'STUDENT', 'NORMAL',    '123456', '400.00', 1200),
    (101, '202601010005', '2026010105', '刘同学', 'STUDENT', 'NORMAL',    '123456', '200.00', 0),
    (102, '202601010006', '2026010106', '周同学', 'STUDENT', 'FROZEN',    '123456', '300.00', 0),
    (103, '202601010007', '2026010107', '吴同学', 'STUDENT', 'CANCELLED', '123456', '0.00',   0),
    (104, 'T20260002',    'T2026002',   '孙老师', 'TEACHER', 'NORMAL',    '888888', '800.00', 500),
    (105, 'T20260003',    'T2026003',   '郑老师', 'TEACHER', 'LOST',      '888888', '120.00', 0),
]

# id, 终端号, 类型, 位置, 厂商, 密钥, 状态
TERMINALS = [
    (6, 'POS-03',    'POS',         '第二食堂 1 号窗口',  '新中新', 'pos-secret-03',    'ENABLED'),
    (7, 'MART-02',   'SUPERMARKET', '校园超市 2 号收银台', '正方',   'mart-secret-02',   'ENABLED'),
    (8, 'ACCESS-02', 'ACCESS',      '教学楼 A 栋门禁',     '达实',   'access-secret-02', 'DISABLED'),
]

TERM_BY_ID = {
    1: ('POS-01', 'POS'), 2: ('POS-02', 'POS'), 3: ('MART-01', 'SUPERMARKET'),
    4: ('ACCESS-01', 'ACCESS'), 5: ('WATER-01', 'WATER'),
    6: ('POS-03', 'POS'), 7: ('MART-02', 'SUPERMARKET'), 8: ('ACCESS-02', 'ACCESS'),
}
CARD_BY_ID = {c[0]: c for c in CARDS}

# ---------------------------------------------------------------- 交易流水
# id / day(距今几天) / 时间 / 卡 / 终端 / 类型 / 原始金额 / 优惠 / 策略 / 状态 / 原交易 / 备注 / 明细
TXS = [
    dict(id=100, day=4, time='08:30:00', card=100, term=None, typ='RECHARGE', amount='300.00', disc='0.00', st='NORMAL',   status='SUCCESS',  orig=None, remark='柜台现金充值', items=[]),
    dict(id=101, day=4, time='09:10:00', card=101, term=None, typ='RECHARGE', amount='100.00', disc='0.00', st='NORMAL',   status='SUCCESS',  orig=None, remark='自助机充值', items=[]),
    dict(id=102, day=4, time='11:42:10', card=100, term=1, typ='CONSUME', amount='18.00', disc='1.80', st='DISCOUNT', status='SUCCESS', orig=None, remark='学生 9 折', items=[('一荤一素套餐', 1, '12.00'), ('豆浆', 2, '3.00')]),
    dict(id=103, day=4, time='12:05:33', card=104, term=2, typ='CONSUME', amount='40.00', disc='2.00', st='DISCOUNT', status='SUCCESS', orig=None, remark='教师 95 折', items=[('教工套餐', 2, '20.00')]),
    dict(id=104, day=4, time='15:20:04', card=101, term=3, typ='CONSUME', amount='25.00', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='超市日用品', items=[('笔记本', 1, '15.00'), ('矿泉水', 2, '5.00')]),
    dict(id=105, day=4, time='18:03:41', card=100, term=4, typ='CONSUME', amount='0.10', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='图书馆门禁', items=[('门禁通行费', 1, '0.10')]),
    dict(id=106, day=4, time='21:35:12', card=104, term=5, typ='CONSUME', amount='0.60', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='宿舍热水', items=[('热水', 30, '0.02')]),

    dict(id=107, day=3, time='11:58:22', card=100, term=1, typ='CONSUME', amount='22.00', disc='2.00', st='SUBSIDY', status='SUCCESS', orig=None, remark='午餐补贴 2 元', items=[('午餐套餐', 1, '22.00')]),
    dict(id=108, day=3, time='12:11:08', card=101, term=6, typ='CONSUME', amount='15.00', disc='1.50', st='DISCOUNT', status='SUCCESS', orig=None, remark='学生 9 折', items=[('二食堂套餐', 1, '15.00')]),
    dict(id=109, day=3, time='14:26:55', card=104, term=7, typ='CONSUME', amount='68.00', disc='3.40', st='DISCOUNT', status='SUCCESS', orig=None, remark='教师 95 折', items=[('办公用品', 1, '48.00'), ('饮用水', 4, '5.00')]),
    dict(id=110, day=3, time='19:40:17', card=100, term=5, typ='CONSUME', amount='1.20', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='宿舍热水', items=[('热水', 60, '0.02')]),
    dict(id=111, day=3, time='20:05:49', card=101, term=4, typ='CONSUME', amount='0.10', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='图书馆门禁', items=[('门禁通行费', 1, '0.10')]),
    dict(id=112, day=3, time='20:31:03', card=100, term=3, typ='CONSUME', amount='32.00', disc='5.00', st='POINTS',  status='SUCCESS', orig=None, remark='积分抵扣 5 元', items=[('洗护用品', 1, '32.00')]),

    dict(id=113, day=2, time='09:02:37', card=104, term=None, typ='RECHARGE', amount='200.00', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='线上充值', items=[]),
    dict(id=114, day=2, time='11:47:19', card=100, term=2, typ='CONSUME', amount='26.00', disc='2.60', st='DISCOUNT', status='SUCCESS', orig=None, remark='学生 9 折', items=[('套餐', 1, '20.00'), ('酸奶', 2, '3.00')]),
    dict(id=115, day=2, time='13:18:44', card=101, term=3, typ='CONSUME', amount='12.50', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='超市文具', items=[('签字笔', 5, '2.50')]),
    dict(id=116, day=2, time='17:52:06', card=104, term=6, typ='CONSUME', amount='55.00', disc='2.75', st='DISCOUNT', status='SUCCESS', orig=None, remark='教师 95 折', items=[('教工套餐', 2, '20.00'), ('水果', 3, '5.00')]),
    dict(id=117, day=2, time='18:20:31', card=100, term=4, typ='CONSUME', amount='0.10', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='图书馆门禁', items=[('门禁通行费', 1, '0.10')]),
    dict(id=118, day=2, time='22:07:58', card=101, term=5, typ='CONSUME', amount='0.40', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='宿舍热水', items=[('热水', 20, '0.02')]),

    dict(id=119, day=1, time='11:36:12', card=100, term=1, typ='CONSUME', amount='20.00', disc='0.00', st='NORMAL', status='REVERSED', orig=None, remark='已冲正（原交易）', items=[('午餐套餐', 1, '20.00')]),
    dict(id=120, day=1, time='11:52:40', card=100, term=1, typ='REFUND', amount='20.00', disc='0.00', st='NORMAL', status='SUCCESS', orig=119, remark='重复扣款冲正', items=[]),
    dict(id=121, day=1, time='12:14:29', card=101, term=2, typ='CONSUME', amount='16.00', disc='1.60', st='DISCOUNT', status='SUCCESS', orig=None, remark='学生 9 折', items=[('套餐', 1, '16.00')]),
    dict(id=122, day=1, time='16:45:03', card=104, term=7, typ='CONSUME', amount='88.00', disc='4.40', st='DISCOUNT', status='SUCCESS', orig=None, remark='教师 95 折', items=[('办公用品', 2, '44.00')]),
    dict(id=123, day=1, time='19:28:36', card=101, term=4, typ='CONSUME', amount='0.10', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='图书馆门禁', items=[('门禁通行费', 1, '0.10')]),
    dict(id=124, day=1, time='20:03:11', card=100, term=1, typ='CONSUME', amount='9999.00', disc='0.00', st='NORMAL', status='FAILED', orig=None, remark='余额不足，责任链拒绝', items=[]),

    dict(id=125, day=0, time='08:41:26', card=100, term=6, typ='CONSUME', amount='24.00', disc='2.40', st='DISCOUNT', status='SUCCESS', orig=None, remark='学生 9 折', items=[('早餐套餐', 2, '12.00')]),
    dict(id=126, day=0, time='10:15:09', card=104, term=5, typ='CONSUME', amount='0.80', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='宿舍热水', items=[('热水', 40, '0.02')]),
    dict(id=127, day=0, time='11:22:47', card=101, term=3, typ='CONSUME', amount='9.90', disc='0.00', st='NORMAL', status='SUCCESS', orig=None, remark='超市零食', items=[('面包', 1, '6.90'), ('牛奶', 1, '3.00')]),
]

for t in TXS:
    t['amount_d'] = D(t['amount'])
    t['disc_d'] = D(t['disc'])
    t['actual_d'] = t['amount_d'] - t['disc_d']
    t['tx_no'] = 'DEMO-TX-%d' % t['id']
    t['idem'] = 'DEMO-KEY-%d' % t['id']
    t['orig_no'] = 'DEMO-TX-%d' % t['orig'] if t['orig'] else None
    # 明细小计必须等于原始金额
    total = sum((D(str(p)) * q for _, q, p in t['items']), D('0'))
    if t['items']:
        assert total == t['amount_d'], '%s items %s != amount %s' % (t['tx_no'], total, t['amount_d'])
    if t['typ'] in ('RECHARGE', 'REFUND'):
        assert t['disc_d'] == 0 and not t['items']

# ---------------------------------------------------------------- 派生账目
balance = {c[0]: D(c[7]) for c in CARDS}
points = {c[0]: int(c[8]) for c in CARDS}
version = {c[0]: 0 for c in CARDS}
points_rows, notify_rows, item_rows = [], [], []
consume_total = recharge_total = refund_total = D('0')
settle_groups = {}

for t in TXS:
    if t['status'] != 'SUCCESS':
        # 失败/已冲正交易只留流水记录，不进账，也不触发 afterCommit 观察者
        continue

    if t['typ'] == 'RECHARGE':
        balance[t['card']] += t['actual_d']
        recharge_total += t['actual_d']
    elif t['typ'] == 'CONSUME':
        balance[t['card']] -= t['actual_d']
        consume_total += t['actual_d']
    elif t['typ'] == 'REFUND':
        balance[t['card']] += t['actual_d']
        refund_total += t['actual_d']
    version[t['card']] += 1

    # 积分（与 PointsUtils.delta 同规则）
    if t['typ'] == 'CONSUME':
        delta = int(-t['disc_d'] * 100) if t['st'] == 'POINTS' else int(t['actual_d'])
        points[t['card']] += delta
        if delta != 0:                      # 与 PointsObserver 的 delta == 0 提前返回一致
            points_rows.append((t, delta, points[t['card']]))

    # 交易明细
    for name, qty, price in t['items']:
        item_rows.append((t, name, qty, D(str(price)), D(str(price)) * qty))

    # 通知（PUSH）+ 特殊场景审计留痕（AUDIT）
    card_no = CARD_BY_ID[t['card']][1]
    notify_rows.append((t, 'PUSH', '卡号 %s 完成 %s，金额 %s 元，交易号 %s'
                        % (card_no, t['typ'], f2(t['actual_d']), t['tx_no'])))
    if t['st'] == 'POINTS' or t['typ'] == 'REFUND':
        notify_rows.append((t, 'AUDIT', '交易 %s 已完成结算增强：积分累计与操作留痕' % t['tx_no']))

    # 对账汇总：口径与 ReconcileDao.settle 完全一致
    if t['typ'] in ('CONSUME', 'REFUND') and t['term'] is not None:
        key = (t['term'], t['day'])
        cnt, amt = settle_groups.get(key, (0, D('0')))
        settle_groups[key] = (cnt + 1, amt + t['actual_d'])

assert all(b >= 0 for b in balance.values()), '出现负余额'
assert all(p >= 0 for p in points.values()), '出现负积分'

# ---------------------------------------------------------------- 生成 SQL
def esc(s):
    return str(s).replace('\\', '\\\\').replace("'", "''")


def ts(day, tmpl):
    return "CONCAT(DATE_SUB(CURDATE(), INTERVAL %d DAY), ' %s')" % (day, tmpl)


def date_expr(day):
    return 'DATE_SUB(CURDATE(), INTERVAL %d DAY)' % day


L = []
L.append(BEGIN)
L.append('-- 扩展测试数据：新增卡片 / 终端 / 历史流水 / 明细 / 通知 / 积分 / 对账记录')
L.append('-- 由数据生成脚本（scripts/generate_test_data.py）统一产出，请勿手工编辑本段；全部 INSERT IGNORE，可重复执行。')
L.append('-- 时间用相对日期，保证任何时候部署都落在统计页的「近 30 日」窗口内。')
L.append('')
L.append('-- 6 张扩展卡：覆盖 NORMAL / FROZEN / CANCELLED / LOST 四种状态与两种卡类型')
L.append('INSERT IGNORE INTO card (id, card_no, student_no, holder_name, card_type, status, pin, version) VALUES')
L.append(',\n'.join(' (%d, \'%s\', \'%s\', \'%s\', \'%s\', \'%s\', \'%s\', 0)'
                    % (c[0], c[1], c[2], esc(c[3]), c[4], c[5], c[6]) for c in CARDS) + ';')
L.append('')
L.append('-- 账户余额 = 期初余额 + 充值 - 成功消费 + 退款，与下方历史流水严格对账')
L.append('INSERT IGNORE INTO account (id, card_id, balance, points, frozen_amount, version) VALUES')
L.append(',\n'.join(' (%d, %d, %s, %d, 0.00, %d)'
                    % (c[0], c[0], f2(balance[c[0]]), points[c[0]], version[c[0]]) for c in CARDS) + ';')
L.append('')
L.append('-- 3 个扩展终端（ACCESS-02 为停用终端，用于演示校验链拒绝）')
L.append('INSERT IGNORE INTO terminal (id, terminal_no, terminal_type, location, vendor, secret, status) VALUES')
L.append(',\n'.join(' (%d, \'%s\', \'%s\', \'%s\', \'%s\', \'%s\', \'%s\')'
                    % (t[0], t[1], t[2], esc(t[3]), esc(t[4]), t[5], t[6]) for t in TERMINALS) + ';')
L.append('')
L.append('-- %d 笔历史交易：充值 / 消费 / 退款，覆盖 4 种终端类别与 4 种优惠策略' % len(TXS))
L.append('INSERT IGNORE INTO transaction (id, tx_no, card_id, card_no, terminal_id, terminal_no, tx_type, '
         'amount, discount_amount, actual_amount, strategy_type, status, idempotency_key, original_tx_no, '
         'remark, operator, created_at, settled_at) VALUES')
rows = []
for t in TXS:
    term = TERM_BY_ID.get(t['term'])
    term_id = str(t['term']) if t['term'] is not None else 'NULL'
    term_no = "'%s'" % term[0] if term else 'NULL'
    orig = "'%s'" % t['orig_no'] if t['orig_no'] else 'NULL'
    settled = ts(t['day'], '23:30:00') if t['status'] == 'SUCCESS' else 'NULL'
    op = "'%s'" % (term[0] if term else 'admin')
    rows.append(' (%d, \'%s\', %d, \'%s\', %s, %s, \'%s\', %s, %s, %s, \'%s\', \'%s\', \'%s\', %s, \'%s\', %s, %s, %s)'
                % (t['id'], t['tx_no'], t['card'], CARD_BY_ID[t['card']][1], term_id, term_no, t['typ'],
                   f2(t['amount_d']), f2(t['disc_d']), f2(t['actual_d']), t['st'], t['status'],
                   t['idem'], orig, esc(t['remark']), op, ts(t['day'], t['time']), settled))
L.append(',\n'.join(rows) + ';')
L.append('')
L.append('-- %d 条交易明细（建造者模式产物，小计之和等于主表原始金额）' % len(item_rows))
L.append('INSERT IGNORE INTO transaction_item (id, tx_id, tx_no, item_name, quantity, unit_price, amount) VALUES')
L.append(',\n'.join(' (%d, %d, \'%s\', \'%s\', %d, %s, %s)'
                    % (100 + i, t['id'], t['tx_no'], esc(name), qty, f2(price), f2(amount))
                    for i, (t, name, qty, price, amount) in enumerate(item_rows)) + ';')
L.append('')
L.append('-- %d 条通知与审计留痕（观察者模式产出）' % len(notify_rows))
L.append('INSERT IGNORE INTO notify_log (id, tx_no, channel, content, status, created_at) VALUES')
L.append(',\n'.join(' (%d, \'%s\', \'%s\', \'%s\', \'SENT\', %s)'
                    % (100 + i, t['tx_no'], ch, esc(content), ts(t['day'], t['time']))
                    for i, (t, ch, content) in enumerate(notify_rows)) + ';')
L.append('')
L.append('-- %d 条积分流水（与 PointsUtils.delta 规则一致）' % len(points_rows))
L.append('INSERT IGNORE INTO points_log (id, card_id, tx_no, points_change, balance_after, created_at) VALUES')
L.append(',\n'.join(' (%d, %d, \'%s\', %d, %d, %s)'
                    % (100 + i, t['card'], t['tx_no'], delta, after, ts(t['day'], t['time']))
                    for i, (t, delta, after) in enumerate(points_rows)) + ';')
L.append('')
L.append('-- %d 条终端对账记录，口径与 GET/POST /api/reconcile/settle 完全一致' % len(settle_groups))
L.append('INSERT IGNORE INTO reconcile_record (id, terminal_id, settle_date, tx_count, total_amount, status, created_at) VALUES')
L.append(',\n'.join(' (%d, %d, %s, %d, %s, \'BALANCED\', %s)'
                    % (100 + i, term_id, date_expr(day), cnt, f2(amt), ts(day, '23:59:00'))
                    for i, ((term_id, day), (cnt, amt)) in enumerate(sorted(settle_groups.items()))) + ';')
L.append(END)

block = '\n'.join(L) + '\n'

# ---------------------------------------------------------------- 注入两份 SQL
def inject(path):
    src = open(path, encoding='utf-8').read()
    if BEGIN in src and END in src:
        src = re.sub(re.escape(BEGIN) + r'.*?' + re.escape(END) + r'[ \t]*\n?', '', src, flags=re.S).rstrip() + '\n'
    open(path, 'w', encoding='utf-8', newline='\n').write(src + '\n' + block)
    print('injected:', os.path.relpath(path, BASE), 'lines', block.count('\n'))


inject(INIT)
inject(DATA)

summary = {
    'cards': len(CARDS), 'terminals': len(TERMINALS), 'transactions': len(TXS),
    'items': len(item_rows), 'notifies': len(notify_rows), 'points_logs': len(points_rows),
    'reconcile_records': len(settle_groups),
    'consume_total': f2(consume_total), 'recharge_total': f2(recharge_total),
    'refund_total': f2(refund_total),
    'status_breakdown': {s: sum(1 for t in TXS if t['status'] == s)
                         for s in ('SUCCESS', 'REVERSED', 'FAILED')},
    'strategy_breakdown': {s: sum(1 for t in TXS if t['st'] == s and t['typ'] == 'CONSUME')
                           for s in ('NORMAL', 'DISCOUNT', 'SUBSIDY', 'POINTS')},
    'balances': {str(c[0]): f2(balance[c[0]]) for c in CARDS},
    'points': {str(c[0]): points[c[0]] for c in CARDS},
}
if IN_BUILD:
    with open(os.path.join(HERE, 'test_data_summary.json'), 'w', encoding='utf-8') as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
print(json.dumps(summary, ensure_ascii=False, indent=2))
