<template>
  <div class="rag-view">
    <h1>RAG 知识库管理</h1>
    <p>这里管理和使用 RAG 知识库。</p>
    <!-- 后续将添加知识库上传、检索等功能 -->
  </div>
</template>

<script setup>
// 可以在这里导入和使用 RAG API
// import { ref, onMounted } from 'vue';
// import api from '@/api';

// const knowledgeBases = ref([]);

// onMounted(async () => {
//   try {
//     // const response = await api.rag.getKnowledgeBases();
//     // knowledgeBases.value = response.data;
//   } catch (error) {
//     console.error("获取知识库列表失败:", error);
//   }
// });
</script>

<style scoped>
.rag-view {
  padding: 20px;
  text-align: center;
}
</style>
