<template>
  <div class="home">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card">
          <h1>欢迎使用校园知识库系统</h1>
          <p>这里汇集了校园内的重要知识和信息，您可以轻松查找和浏览所需的内容。</p>
          <div class="action-buttons">
            <el-button type="primary" size="large" @click="router.push('/chat')">
              <el-icon><ChatDotSquare /></el-icon>
              开始对话
            </el-button>
            <el-input
              v-model="searchQuery"
              placeholder="请输入关键词搜索..."
              class="search-input"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="mt-4">
      <el-col :span="8" v-for="category in categories" :key="category.id">
        <el-card class="category-card">
          <template #header>
            <div class="card-header">
              <span>{{ category.name }}</span>
              <el-button class="button" text @click="navigateToCategory(category.id)">
                查看更多
              </el-button>
            </div>
          </template>
          <div v-loading="loading" class="card-content">
            <ul class="article-list">
              <li v-for="article in category.articles" :key="article.id" @click="viewArticle(article)">
                {{ article.title }}
              </li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ChatDotSquare } from '@element-plus/icons-vue'
import { knowledgeBaseApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const searchQuery = ref('')
const categories = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await knowledgeBaseApi.getCategories()
    categories.value = response.data
    await loadCategoryArticles()
  } catch (error) {
    ElMessage.error('加载分类信息失败')
  } finally {
    loading.value = false
  }
})

const loadCategoryArticles = async () => {
  for (const category of categories.value) {
    try {
      const response = await knowledgeBaseApi.getCategoryArticles(category.id, 1, 3)
      category.articles = response.data
    } catch (error) {
      console.error(`加载分类 ${category.id} 的文章失败:`, error)
    }
  }
}

const handleSearch = () => {
  if (searchQuery.value) {
    router.push({
      name: 'search',
      query: { q: searchQuery.value }
    })
  }
}

const navigateToCategory = (categoryId) => {
  router.push({
    name: 'categories',
    params: { category: categoryId }
  })
}

const viewArticle = (article) => {
  // TODO: 实现文章查看功能
  console.log('查看文章:', article)
}
</script>

<style scoped>
.home {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-card {
  margin-bottom: 20px;
  text-align: center;
}

.welcome-card h1 {
  margin-bottom: 1rem;
  color: #303133;
}

.welcome-card p {
  color: #606266;
  margin-bottom: 2rem;
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 1rem;
}

.search-input {
  max-width: 600px;
  margin: 0 auto;
}

.mt-4 {
  margin-top: 20px;
}

.category-card {
  margin-bottom: 20px;
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.article-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.article-list li {
  padding: 8px 0;
  border-bottom: 1px solid #eee;
  color: #606266;
  cursor: pointer;
}

.article-list li:last-child {
  border-bottom: none;
}

.article-list li:hover {
  color: #409EFF;
}

.card-content {
  min-height: 150px;
}
</style>
