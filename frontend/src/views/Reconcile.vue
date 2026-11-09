<template>
  <div>
    <div class="card">
      <h2>终端对账结算</h2>
      <div class="flex">
        <div style="width:180px"><label>开始日期</label><input v-model="startDate" type="date" /></div>
        <div style="width:180px"><label>结束日期</label><input v-model="endDate" type="date" /></div>
        <button style="margin-top:18px" @click="settle">生成对账单</button>
        <button class="ghost" style="margin-top:18px" @click="loadRecords">查看历史记录</button>
      </div>
    </div>
    <div v-if="msg" class="alert" :class="ok ? 'ok' : 'err'">{{ msg }}</div>
    <div class="grid grid-2">
      <div class="card">
        <h2>按终端汇总</h2>
        <div class="bar-wrap" v-if="bars.length">
          <div class="bar-item" v-for="b in bars" :key="b.name">
            <div class="bar-val">¥{{ b.value }}</div>
            <div class="bar" :style="{ height: b.height + 'px' }"></div>
            <div class="name">{{ b.name }}</div>
          </div>
        </div>
        <div v-else class="muted">点击「生成对账单」后展示按终端的交易金额对比。</div>
      </div>
      <div class="card">
        <h2>对账结果明细</h2>
        <div class="table-wrap">
          <table>
            <thead><tr><th>终端</th><th>类别</th><th>日期</th><th>笔数</th><th>金额合计</th></tr></thead>
            <tbody>
              <tr v-for="(r, i) in rows" :key="i">
                <td class="mono">{{ r.terminal_no || r.terminalNo }}</td><td>{{ r.terminal_type || r.terminalType || '—' }}</td>
                <td>{{ r.settle_date || r.settleDate }}</td><td>{{ r.tx_count ?? r.txCount }}</td><td>{{ r.total_amount ?? r.totalAmount }}</td>
              </tr>
              <tr v-if="!rows.length"><td colspan="5" class="empty">暂无对账数据</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  data() {
    return { startDate: '', endDate: '', rows: [], bars: [], msg: '', ok: true }
  },
  methods: {
    async settle() {
      try {
        const params = {}
        if (this.startDate) params.startDate = this.startDate
        if (this.endDate) params.endDate = this.endDate
        this.rows = await api.settle(params) || []
        this.buildBars()
        this.show('对账完成，共 ' + this.rows.length + ' 组终端汇总', true)
      } catch (e) { this.show(e.message, false) }
    },
    async loadRecords() {
      try {
        this.rows = await api.records() || []
        this.buildBars()
        this.show('已加载 ' + this.rows.length + ' 条对账记录', true)
      } catch (e) { this.show(e.message, false) }
    },
    buildBars() {
      const values = this.rows.map(r => Number(r.total_amount ?? r.totalAmount ?? 0))
      const max = Math.max(1, ...values)
      this.bars = this.rows.slice(0, 6).map((r, i) => ({
        name: r.terminal_no || r.terminalNo || ('终端' + (i + 1)),
        value: values[i].toFixed(2),
        height: 20 + Math.round(values[i] / max * 100)
      }))
    },
    show(text, ok) { this.msg = text; this.ok = ok; setTimeout(() => { this.msg = '' }, 6000) }
  }
}
</script>
