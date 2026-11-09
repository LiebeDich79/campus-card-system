# 前端工程说明（Vue 3 + Vite）

## 运行

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

后端需先启动（Spring Boot，8080 端口）。Vite 已配置 `/api` 代理到 `http://localhost:8080`，无需处理跨域。

## 页面与后端接口对应

| 页面 | 主要功能 | 调用接口 |
| --- | --- | --- |
| 总览看板 Dashboard | 交易笔数、消费/充值合计、按终端汇总、通知记录 | `GET /api/statistics/summary`、`GET /api/notifications` |
| 卡片管理 Cards | 开户、充值、挂失、销户、卡片列表 | `POST/GET/PUT/DELETE /api/cards`、`POST /api/cards/{cardNo}/recharge` |
| 消费收银台 Consume | 标准消费、异构终端报文消费、退款冲正 | `POST /api/transactions/consume`、`/consume/raw`、`/refund` |
| 交易流水 Transactions | 多条件分页查询 | `GET /api/transactions` |
| 对账统计 Reconcile | 按终端对账、历史记录、金额对比图 | `POST /api/reconcile/settle`、`GET /api/reconcile/records` |

## 说明

- 使用 Vue 3 + Vue Router 4 + Axios，单文件组件（SFC），路由懒加载。
- 所有请求经 `src/api/index.js` 统一封装，业务错误码由拦截器转成 `Error(message)` 交给页面提示。
- 若不方便执行 `npm install`，可直接双击工程根目录下的 `界面预览.html` 查看界面布局与交互演示（使用 Vue 3 CDN，需联网）。
