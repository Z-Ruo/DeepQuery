<script>
export default {
  name: 'App'
}
</script>

<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<style>
/* 全局样式 - 禁用body滚动条 */
body {
  overflow: hidden;
  margin: 0;
  padding: 0;
  height: 100vh;
}

/* 确保#app占满整个视口 */
#app {
  height: 100vh;
  overflow: hidden;
}
</style>


