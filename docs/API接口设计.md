# 接口设计（REST API）

后端基于 Spring Boot 2.7，所有接口统一前缀 `/api`，返回体为统一包装：

```json
{ "code": 0, "message": "ok", "data": { } }
```

- `code = 0` 表示成功；非 0 表示业务异常，`message` 给出可读错误原因。
- 常见业务码：`1005` 余额不足、`1007` 重复请求（幂等键已使用）、`1008` 终端鉴权失败、`1009` 限流、`1010` 并发冲突，详见 `src/main/java/com/campus/card/common/ErrorCode.java`。

> **终端接入鉴权**：当请求头携带 `X-Terminal-No` 与 `X-Terminal-Sign` 时，代理层会校验终端是否注册、启用且签名合法；浏览器管理端（不带这两个头）不受影响。
> **幂等**：消费 / 充值 / 退款请求建议携带 `idempotencyKey`，同一键值仅允许成功一次，重试请更换新键。

---

## 一、卡片与账户（CardController → `/api/cards`）

| 方法 | 路径 | 说明 | 关键参数 |
| --- | --- | --- | --- |
| POST | `/api/cards?balance=0` | 开户 | body: `Card`；`balance`：初始余额（默认 0） |
| GET | `/api/cards` | 卡片列表 | — |
| GET | `/api/cards/{cardNo}` | 卡片详情 / 状态查询 | 路径参数 `cardNo` |
| PUT | `/api/cards/{cardNo}/lost` | 挂失 | 路径参数 `cardNo` |
| DELETE | `/api/cards/{cardNo}` | 销户 | 路径参数 `cardNo`（余额必须为 0） |
| POST | `/api/cards/{cardNo}/recharge` | 充值 | 路径 `cardNo` + body `RechargeRequest` |

**开户请求示例**

```http
POST /api/cards?balance=100
Content-Type: application/json

{ "cardNo": "202601010099", "holderName": "新同学", "cardType": "STUDENT", "pin": "123456" }
```

**响应**

```json
{ "code": 0, "message": "ok",
  "data": { "id": 11, "cardNo": "202601010099", "holderName": "新同学",
            "cardType": "STUDENT", "status": "NORMAL" } }
```

**充值请求示例**

```http
POST /api/cards/202601010099/recharge
Content-Type: application/json

{ "amount": 50.00, "idempotencyKey": "RECHARGE-001", "operator": "admin" }
```

---

## 二、交易（TransactionController → `/api/transactions`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/transactions/consume` | 标准消费扣款（经策略 + 责任链） |
| POST | `/api/transactions/consume/raw` | 异构终端原始报文消费（经适配器） |
| POST | `/api/transactions/refund` | 退款 / 冲正 |

**消费请求（ConsumptionRequest）**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| cardNo | String | 是 | 卡号 |
| terminalNo | String | 否 | 终端编号 |
| terminalType | String | 否 | POS / SUPERMARKET / ACCESS / WATER |
| amount | BigDecimal | 是 | 原始金额（> 0） |
| pin | String | 否 | 交易密码（管理端可不传） |
| strategyType | String | 否 | 指定优惠策略：NORMAL/DISCOUNT/SUBSIDY/POINTS |
| idempotencyKey | String | 否（建议） | 幂等键 |
| operator | String | 否 | 操作员 |
| items | List | 否 | 消费明细（品名/数量/单价/小计） |

**消费请求示例**

```http
POST /api/transactions/consume
Content-Type: application/json

{ "cardNo": "202601010001", "terminalNo": "POS-01", "terminalType": "POS",
  "amount": 15.00, "pin": "123456", "idempotencyKey": "DEMO-001" }
```

**消费响应（TxResult）**

```json
{ "code": 0, "message": "消费扣款成功",
  "data": { "txNo": "TX2026110900001", "txType": "CONSUME",
            "amount": 15.00, "discountAmount": 1.50, "actualAmount": 13.50,
            "strategyType": "DISCOUNT", "balance": 486.50, "status": "SUCCESS" } }
```

> 学生卡默认命中 **DISCOUNT（9 折）** → 实付 13.50；若处于 11:00–13:00 食堂 POS 时段，自动命中 **SUBSIDY（补贴 2 元）** → 实付 13.00。

**退款请求（RefundRequest）**：`{ "originalTxNo": "TX...", "amount": 15.00, "reason": "误刷", "idempotencyKey": "REF-001" }`。
仅对 `SUCCESS` 且 `CONSUME` 的交易可退；不传 `amount` 则按原实付全额退回，并回补账户余额。

---

## 三、查询（QueryController → `/api`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/transactions` | 交易明细分页查询 |
| GET | `/api/statistics/summary` | 分类统计（看板数据） |
| GET | `/api/notifications` | 通知与操作审计日志 |

- `/api/transactions?cardNo=&terminalType=&startDate=&endDate=&page=1&size=10`
  返回 `{ list, total, page, size }`；不传日期默认近 30 日。
- `/api/statistics/summary` 返回 `{ txCount, consumeTotal, rechargeTotal, netAmount, byType, notices }`。
- `/api/notifications` 返回观察者产生的推送（PUSH）与审计（AUDIT）记录列表。

---

## 四、对账与终端（ReconcileController / TerminalController）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/reconcile/settle?startDate=&endDate=` | 按终端汇总对账 |
| GET | `/api/reconcile/records` | 历史对账记录 |
| GET | `/api/terminals` | 终端列表 |

`/api/reconcile/settle` 默认按当天汇总各终端的笔数与金额合计，返回列表形如
`[{ terminal, terminalType, settleDate, txCount, totalAmount }]`。
