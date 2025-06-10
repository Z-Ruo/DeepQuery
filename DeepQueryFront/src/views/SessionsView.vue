<template>
  <div class="sessions-view">
    <h1>会话历史</h1>
    <p>这里查看和管理用户的会话历史。</p>
    <!-- 后续将添加会话列表展示和管理功能 -->
  </div>
</template>

<script setup>
// 可以在这里导入和使用 session API
// import { ref, onMounted } from 'vue';
// import api from '@/api';
// import { getCurrentUsername } from '@/api/auth'; // 或从 store 获取 userId

// const sessions = ref([]);

// onMounted(async () => {
//   try {
//     // const userId = ...; // 获取当前用户ID
//     // const response = await api.session.listSessions(userId);
//     // sessions.value = response.data;
//   } catch (error) {
//     console.error("获取会话列表失败:", error);
//   }
// });
</script>

<style scoped>
.sessions-view {
  padding: 20px;
  text-align: center;
}
</style>
