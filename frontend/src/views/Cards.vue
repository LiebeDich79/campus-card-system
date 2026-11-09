<template>
  <div>
    <div class="card">
      <h2>开户</h2>
      <div class="form-row-3">
        <div><label>卡号</label><input v-model="form.cardNo" placeholder="如 202601010099" /></div>
        <div><label>持卡人</label><input v-model="form.holderName" placeholder="姓名" /></div>
        <div><label>学号/工号</label><input v-model="form.studentNo" /></div>
        <div><label>卡类型</label><select v-model="form.cardType"><option value="STUDENT">学生卡</option><option value="TEACHER">教师卡</option></select></div>
        <div><label>交易密码</label><input v-model="form.pin" placeholder="6 位数字" /></div>
        <div><label>初始余额（元）</label><input v-model.number="balance" type="number" step="0.01" /></div>
      </div>
      <button @click="open">提交开户</button>
      <span class="muted" style="margin-left:10px">开户会同步创建账户（业务层事务保证一致）</span>
    </div>
    <div v-if="msg" class="alert" :class="ok ? 'ok' : 'err'">{{ msg }}</div>
    <div class="card">
      <h2>卡片列表</h2>
      <div class="flex" style="margin-bottom:12px">
        <button class="ghost" @click="load">刷新</button>
        <span class="muted right">状态：NORMAL 正常 / LOST 挂失 / FROZEN 冻结 / CANCELLED 注销</span>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>卡号</th><th>持卡人</th><th>类型</th><th>状态</th><th>充值（元）</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="c in cards" :key="c.cardNo">
              <td class="mono">{{ c.cardNo }}</td><td>{{ c.holderName }}</td><td>{{ c.cardType }}</td>
              <td><span class="tag" :class="c.status === 'NORMAL' ? '' : 'warn'">{{ c.status }}</span></td>
              <td><input v-model.number="amounts[c.cardNo]" type="number" step="0.01" placeholder="金额" style="width:110px" /></td>
              <td>
                <button class="ghost" @click="recharge(c)">充值</button>
                <button class="warn" style="margin-left:6px" @click="lost(c)">挂失</button>
                <button class="danger" style="margin-left:6px" @click="cancel(c)">销户</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  data() {
    return { cards: [], amounts: {}, balance: 0, form: { cardNo: '', holderName: '', studentNo: '', cardType: 'STUDENT', pin: '123456' }, msg: '', ok: true }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      try { this.cards = await api.cards() || [] } catch (e) { this.show(e.message, false) }
    },
    async open() {
      try {
        await api.openCard(this.form, this.balance)
        this.show('开户成功：' + this.form.cardNo, true)
        this.form.cardNo = ''
        this.load()
      } catch (e) { this.show(e.message, false) }
    },
    async recharge(c) {
      const amount = Number(this.amounts[c.cardNo])
      if (!amount || amount <= 0) return this.show('请输入正确的充值金额', false)
      try {
        const r = await api.recharge(c.cardNo, { amount, operator: 'admin', idempotencyKey: 'WEB-R-' + Date.now() })
        this.show('充值成功，交易号 ' + r.txNo + '，余额 ' + r.balance, true)
        this.load()
      } catch (e) { this.show(e.message, false) }
    },
    async lost(c) {
      try { await api.lostCard(c.cardNo); this.show('已挂失：' + c.cardNo, true); this.load() }
      catch (e) { this.show(e.message, false) }
    },
    async cancel(c) {
      try { await api.cancelCard(c.cardNo); this.show('已销户：' + c.cardNo, true); this.load() }
      catch (e) { this.show(e.message, false) }
    },
    show(text, ok) { this.msg = text; this.ok = ok; setTimeout(() => { this.msg = '' }, 6000) }
  }
}
</script>
