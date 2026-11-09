import axios from 'axios'

/** 统一 Axios 实例：开发期由 Vite 代理 /api 到 Spring Boot 8080 端口。 */
const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && body.code !== 0) {
      return Promise.reject(new Error(body.message || '业务处理失败'))
    }
    return body ? body.data : null
  },
  err => Promise.reject(new Error(err.response?.data?.message || err.message || '网络异常'))
)

export default {
  // 卡片与账户
  cards: () => http.get('/cards'),
  card: cardNo => http.get('/cards/' + cardNo),
  openCard: (card, balance) => http.post('/cards?balance=' + (balance || 0), card),
  lostCard: cardNo => http.put('/cards/' + cardNo + '/lost'),
  cancelCard: cardNo => http.delete('/cards/' + cardNo),
  recharge: (cardNo, req) => http.post('/cards/' + cardNo + '/recharge', req),
  // 交易
  consume: req => http.post('/transactions/consume', req),
  consumeRaw: raw => http.post('/transactions/consume/raw', raw),
  refund: req => http.post('/transactions/refund', req),
  // 查询与统计
  transactions: params => http.get('/transactions', { params }),
  summary: params => http.get('/statistics/summary', { params }),
  notifications: () => http.get('/notifications'),
  // 对账与终端
  settle: params => http.post('/reconcile/settle', null, { params }),
  records: () => http.get('/reconcile/records'),
  terminals: () => http.get('/terminals')
}
