<template>
  <div class="profile-view">
    <h1>用户中心</h1>
    <p>这里是用户的个人信息页面。</p>
    <!-- 后续将添加用户信息展示和编辑功能 -->
  </div>
</template>

<script setup>
// import { ref, onMounted } from 'vue';
// import { getCurrentUsername } from '@/api/auth';

// const username = ref('');

// onMounted(() => {
//   username.value = getCurrentUsername() || '未登录';
// });
</script>

<style scoped>
.profile-view {
  padding: 20px;
  text-align: center;
}
</style>
