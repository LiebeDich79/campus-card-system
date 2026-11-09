<template>
  <div>
    <div class="card">
      <h2>消费明细查询</h2>
      <div class="form-row-3">
        <div><label>卡号</label><input v-model="query.cardNo" placeholder="留空查询全部" /></div>
        <div><label>终端类别</label>
          <select v-model="query.terminalType"><option value="">全部</option><option value="POS">食堂 POS</option><option value="SUPERMARKET">超市</option><option value="ACCESS">门禁</option><option value="WATER">水控</option></select>
        </div>
        <div><label>开始日期</label><input v-model="query.startDate" type="date" /></div>
        <div><label>结束日期</label><input v-model="query.endDate" type="date" /></div>
        <div><label>每页条数</label><input v-model.number="query.size" type="number" min="1" max="200" /></div>
        <div style="display:flex;align-items:flex-end"><button @click="load">查询</button></div>
      </div>
    </div>
    <div class="card">
      <h2>交易流水（共 {{ total }} 条，第 {{ query.page }} 页）</h2>
      <div class="table-wrap">
        <table>
          <thead><tr><th>交易号</th><th>卡号</th><th>类型</th><th>终端</th><th>原价</th><th>优惠</th><th>实付</th><th>策略</th><th>状态</th><th>时间</th></tr></thead>
          <tbody>
            <tr v-for="t in list" :key="t.txNo">
              <td><code>{{ t.txNo }}</code></td><td class="mono">{{ t.cardNo }}</td><td>{{ t.txType }}</td><td>{{ t.terminalNo || '—' }}</td>
              <td>{{ t.amount }}</td><td>{{ t.discountAmount }}</td><td><b>{{ t.actualAmount }}</b></td>
              <td>{{ t.strategyType || '—' }}</td>
              <td><span class="tag" :class="t.status === 'SUCCESS' ? '' : 'gray'">{{ t.status }}</span></td>
              <td class="muted">{{ t.createdAt || t.created_at }}</td>
            </tr>
            <tr v-if="!list.length"><td colspan="10" class="empty">暂无数据</td></tr>
          </tbody>
        </table>
      </div>
      <div class="flex" style="margin-top:14px">
        <button class="ghost" :disabled="query.page <= 1" @click="page(-1)">上一页</button>
        <button class="ghost" @click="page(1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  data() {
    return { query: { cardNo: '', terminalType: '', startDate: '', endDate: '', page: 1, size: 10 }, list: [], total: 0 }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      try {
        const params = Object.assign({}, this.query)
        Object.keys(params).forEach(k => { if (params[k] === '') delete params[k] })
        const data = await api.transactions(params) || {}
        this.list = data.list || []
        this.total = data.total || 0
      } catch (e) { console.warn(e.message) }
    },
    page(delta) { this.query.page = Math.max(1, this.query.page + delta); this.load() }
  }
}
</script>
