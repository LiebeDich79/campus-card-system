<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="5" width="20" height="14" rx="2.5" />
            <line x1="2" y1="10" x2="22" y2="10" />
          </svg>
        </div>
        <div>
          <div class="name">校园一卡通</div>
          <div class="sub">消费与结算系统</div>
        </div>
      </div>
      <nav class="menu">
        <router-link v-for="m in menus" :key="m.path" :to="m.path" active-class="active">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path :d="m.icon" />
          </svg>
          <span>{{ m.title }}</span>
        </router-link>
      </nav>
      <div class="foot">
        Spring Boot 2.7 · Java 8<br />
        MySQL 8.0 · 9 种设计模式
      </div>
    </aside>
    <div class="main">
      <header class="topbar">
        <div class="title-wrap">
          <h1>{{ currentTitle }}</h1>
          <div class="crumb">校园一卡通 / {{ currentTitle }}</div>
        </div>
        <div class="user">
          <div class="env">
            当前用户：admin<br />
            后端 http://localhost:8080
          </div>
          <div class="avatar">AD</div>
        </div>
      </header>
      <div class="content">
        <transition name="fade" mode="out-in">
          <router-view />
        </transition>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      menus: [
        { path: '/', title: '总览看板', icon: 'M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z' },
        { path: '/cards', title: '卡片管理', icon: 'M3 5h18v14H3zM3 10h18' },
        { path: '/consume', title: '消费收银台', icon: 'M3 3h2l2.4 12.4a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.6L21 7H6' },
        { path: '/transactions', title: '交易流水', icon: 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01' },
        { path: '/reconcile', title: '对账统计', icon: 'M6 20V10M12 20V4M18 20v-7' }
      ]
    }
  },
  computed: {
    currentTitle() {
      const hit = this.menus.find(m => m.path === this.$route.path)
      return hit ? hit.title : '校园一卡通'
    }
  }
}
</script>
