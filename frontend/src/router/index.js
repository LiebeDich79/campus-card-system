import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '总览看板' } },
  { path: '/cards', name: 'cards', component: () => import('../views/Cards.vue'), meta: { title: '卡片管理' } },
  { path: '/consume', name: 'consume', component: () => import('../views/Consume.vue'), meta: { title: '消费收银台' } },
  { path: '/transactions', name: 'transactions', component: () => import('../views/Transactions.vue'), meta: { title: '交易流水' } },
  { path: '/reconcile', name: 'reconcile', component: () => import('../views/Reconcile.vue'), meta: { title: '对账统计' } }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
