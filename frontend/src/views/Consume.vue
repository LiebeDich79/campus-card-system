<template>
  <div>
    <div class="grid grid-2">
      <div class="card">
        <h2>标准消费扣款（策略 + 责任链 + 工厂/建造者）</h2>
        <div class="form-row">
          <div><label>卡号</label><input v-model="form.cardNo" placeholder="202601010001" /></div>
          <div><label>终端编号</label><input v-model="form.terminalNo" placeholder="POS-01" /></div>
          <div><label>终端类型</label>
            <select v-model="form.terminalType">
              <option value="POS">食堂 POS</option><option value="SUPERMARKET">校园超市</option>
              <option value="ACCESS">门禁</option><option value="WATER">水控</option>
            </select>
          </div>
          <div><label>消费金额（元）</label><input v-model.number="form.amount" type="number" step="0.01" /></div>
          <div><label>优惠策略（留空由系统自动选择）</label>
            <select v-model="form.strategyType">
              <option value="">自动选择</option><option value="NORMAL">普通扣款</option>
              <option value="DISCOUNT">折扣</option><option value="SUBSIDY">午餐补贴</option>
              <option value="POINTS">积分抵扣</option>
            </select>
          </div>
          <div><label>交易密码</label><input v-model="form.pin" placeholder="123456" /></div>
        </div>
        <div class="form-row">
          <div><label>幂等键（防重复消费）</label><input v-model="form.idempotencyKey" /></div>
          <div style="display:flex;align-items:flex-end;gap:8px">
            <button @click="resetKey" class="ghost">生成新键</button>
            <button @click="submit" :disabled="loading">{{ loading ? '处理中…' : '提交消费' }}</button>
          </div>
        </div>
        <p class="muted">同一幂等键重复提交会被责任链第一级拦截并返回 1007。</p>
      </div>

      <div class="card">
        <h2>异构终端原始报文（适配器模式）</h2>
        <div class="form-row">
          <div><label>终端类型</label>
            <select v-model="raw.terminalType">
              <option value="POS">食堂 POS（竖线分隔）</option>
              <option value="SUPERMARKET">超市（JSON）</option>
              <option value="ACCESS">门禁（卡号）</option>
              <option value="WATER">水控（卡号|升数）</option>
            </select>
          </div>
          <div><label>终端编号</label><input v-model="raw.terminalNo" /></div>
        </div>
        <label>原始报文 payload</label>
        <textarea v-model="raw.payload" rows="3"></textarea>
        <div style="margin-top:10px" class="flex">
          <button @click="submitRaw">解析并扣款</button>
          <button class="ghost" @click="fillDemo">填入示例报文</button>
        </div>
        <p class="muted" style="margin-top:10px">各厂商报文格式不同，由 TerminalAdapter 转换为统一 ConsumptionRequest。</p>
      </div>
    </div>

    <div v-if="msg" class="alert" :class="ok ? 'ok' : 'err'">{{ msg }}</div>

    <div class="card">
      <h2>退款 / 冲正</h2>
      <div class="form-row-3">
        <div><label>原交易流水号</label><input v-model="refundForm.originalTxNo" placeholder="TX..." /></div>
        <div><label>退款金额（留空表示全额）</label><input v-model.number="refundForm.amount" type="number" step="0.01" /></div>
        <div style="display:flex;align-items:flex-end"><button @click="submitRefund">提交退款</button></div>
      </div>
    </div>

    <div class="card" v-if="result">
      <h2>最近一次交易结果</h2>
      <table>
        <tbody>
          <tr><th style="width:130px">交易号</th><td><code>{{ result.txNo }}</code></td><th style="width:130px">类型</th><td>{{ result.txType }}</td></tr>
          <tr><th>原价 / 优惠</th><td>{{ result.amount }} / {{ result.discountAmount }}</td><th>实付</th><td><b>{{ result.actualAmount }}</b></td></tr>
          <tr><th>命中策略</th><td>{{ result.strategyType || '—' }}</td><th>扣款后余额</th><td>{{ result.balance }}</td></tr>
          <tr><th>状态</th><td><span class="tag">{{ result.status }}</span></td><th>说明</th><td>{{ result.message }}</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  data() {
    return {
      form: { cardNo: '202601010001', terminalNo: 'POS-01', terminalType: 'POS', amount: 15, strategyType: '', pin: '123456', idempotencyKey: '' },
      raw: { terminalNo: 'POS-01', terminalType: 'POS', payload: '', sign: '' },
      refundForm: { originalTxNo: '', amount: null, reason: '前端发起退款', operator: 'admin' },
      result: null, msg: '', ok: true, loading: false
    }
  },
  mounted() { this.resetKey() },
  methods: {
    resetKey() { this.form.idempotencyKey = 'WEB-' + Date.now() + '-' + Math.floor(Math.random() * 1000) },
    fillDemo() {
      const demos = {
        POS: '202601010001|15.00|123456|米饭:1:2.00;红烧肉:1:13.00',
        SUPERMARKET: '{"cardNo":"202601010001","amount":23.50,"pin":"123456","items":[{"name":"牛奶","qty":2,"price":5.50}]}',
        ACCESS: '202601010001',
        WATER: '202601010001|12.5'
      }
      this.raw.payload = demos[this.raw.terminalType]
      this.raw.terminalNo = ({ POS: 'POS-01', SUPERMARKET: 'MART-01', ACCESS: 'ACCESS-01', WATER: 'WATER-01' })[this.raw.terminalType]
      this.raw.sign = 'WEB-' + this.raw.terminalType + '-' + Date.now()
    },
    async submit() {
      this.loading = true
      try {
        const req = Object.assign({}, this.form, { operator: 'admin' })
        if (!req.strategyType) delete req.strategyType
        this.result = await api.consume(req)
        this.show('消费成功：实付 ' + this.result.actualAmount + ' 元，余额 ' + this.result.balance, true)
      } catch (e) { this.show(e.message, false) } finally { this.loading = false }
    },
    async submitRaw() {
      if (!this.raw.payload) return this.show('请先填入原始报文', false)
      try {
        this.result = await api.consumeRaw(Object.assign({}, this.raw, { vendor: 'WEB' }))
        this.show('适配器解析成功并完成扣款：实付 ' + this.result.actualAmount + ' 元', true)
      } catch (e) { this.show(e.message, false) }
    },
    async submitRefund() {
      try {
        const req = Object.assign({}, this.refundForm, { idempotencyKey: 'WEB-F-' + Date.now() })
        if (!req.amount) delete req.amount
        this.result = await api.refund(req)
        this.show('退款成功，余额 ' + this.result.balance, true)
      } catch (e) { this.show(e.message, false) }
    },
    show(text, ok) { this.msg = text; this.ok = ok; setTimeout(() => { this.msg = '' }, 6000) }
  }
}
</script>
