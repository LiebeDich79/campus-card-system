<template>
  <div>
    <div class="grid grid-4" style="margin-bottom:18px">
      <div class="stat">
        <div class="icon blue">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" />
          </svg>
        </div>
        <div class="meta">
          <div class="label">近 30 日交易笔数</div>
          <div class="value">{{ summary.txCount ?? '—' }}</div>
          <div class="sub">成功 + 失败累计</div>
        </div>
      </div>
      <div class="stat">
        <div class="icon green">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
            <line x1="3" y1="6" x2="21" y2="6" /><path d="M16 10a4 4 0 0 1-8 0" />
          </svg>
        </div>
        <div class="meta">
          <div class="label">消费合计（元）</div>
          <div class="value green">{{ money(summary.consumeTotal) }}</div>
          <div class="sub">人民币（元）</div>
        </div>
      </div>
      <div class="stat">
        <div class="icon orange">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="9" /><polyline points="8 12 12 16 16 12" /><line x1="12" y1="8" x2="12" y2="15" />
          </svg>
        </div>
        <div class="meta">
          <div class="label">充值合计（元）</div>
          <div class="value orange">{{ money(summary.rechargeTotal) }}</div>
          <div class="sub">人民币（元）</div>
        </div>
      </div>
      <div class="stat">
        <div class="icon blue">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3v18" /><path d="M5 7h14" /><path d="M5 7l-2.5 6h5z" /><path d="M19 7l-2.5 6h5z" />
          </svg>
        </div>
        <div class="meta">
          <div class="label">净消费（元）</div>
          <div class="value">{{ money(summary.netAmount) }}</div>
          <div class="sub">消费 − 充值</div>
        </div>
      </div>
    </div>
    <div class="grid grid-2">
      <div class="card">
        <h2>按终端类别的交易汇总</h2>
        <div v-if="!groups.length" class="empty">暂无数据，请先在「消费收银台」发起一笔交易。</div>
        <div class="table-wrap" v-else>
          <table>
            <thead><tr><th>终端类别</th><th>笔数</th><th>金额合计（元）</th></tr></thead>
            <tbody>
              <tr v-for="g in groups" :key="g.terminal_type">
                <td>{{ typeName(g.terminal_type) }}</td><td>{{ g.tx_count }}</td><td>{{ money(g.total_amount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="card">
        <h2>最近消息与通知（观察者模式产出）</h2>
        <div v-if="!notices.length" class="empty">暂无通知记录。</div>
        <div class="table-wrap" v-else>
          <table>
            <thead><tr><th>渠道</th><th>内容</th><th>时间</th></tr></thead>
            <tbody>
              <tr v-for="n in notices" :key="n.id">
                <td><span class="tag" :class="n.channel === 'AUDIT' ? 'gray' : ''">{{ n.channel }}</span></td>
                <td>{{ n.content }}</td><td class="muted">{{ n.created_at }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <div class="card">
      <h2>交易链路一览</h2>
      <p class="muted" style="line-height:2">
        适配器统一终端报文 → 动态代理鉴权/日志/限流 → 责任链逐级校验并原子扣款 → 策略计算实付金额 →
        工厂与建造者生成交易单 → 装饰链完成结算增强 → 事务提交后由观察者通知对账、统计、消息推送与积分流水。
      </p>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  data() {
    return { summary: {}, groups: [], notices: [] }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      try {
        this.summary = await api.summary({}) || {}
        this.groups = this.summary.groupByTerminal || []
        this.notices = await api.notifications() || []
      } catch (e) { this.$root.$emit = null; console.warn(e.message) }
    },
    money(v) { return v === undefined || v === null ? '0.00' : Number(v).toFixed(2) },
    typeName(t) {
      return ({ POS: '食堂 POS', SUPERMARKET: '校园超市', ACCESS: '门禁', WATER: '水控' })[t] || t
    }
  }
}
</script>
