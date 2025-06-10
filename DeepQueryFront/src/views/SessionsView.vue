<template>
  <div class="sessions-view">
    <div class="sessions-header">
      <h1>会话历史</h1>
      <button @click="createNewSession" class="new-session-btn" :disabled="isLoading">
        <i class="icon-plus"></i>
        新建会话
      </button>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>
    
    <!-- 错误信息 -->
    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
      <button @click="loadSessions" class="retry-btn">重试</button>
    </div>
    
    <!-- 会话列表 -->
    <div v-if="!isLoading && !error" class="sessions-container">
      <div v-if="sessions.length === 0" class="empty-state">
        <div class="empty-icon">💬</div>
        <h3>暂无会话记录</h3>
        <p>点击"新建会话"开始您的第一次对话</p>
      </div>
      
      <div v-else class="sessions-list">
        <div 
          v-for="session in sessions" 
          :key="session.sessionId" 
          class="session-card"
          @click="openSession(session)"
        >
          <div class="session-header">
            <h3 class="session-title">
              {{ getSessionTitle(session) }}
            </h3>
            <div class="session-actions">
              <button 
                @click.stop="deleteSession(session.sessionId)" 
                class="delete-btn"
                title="删除会话"
              >
                <i class="icon-delete"></i>
              </button>
            </div>
          </div>
          
          <div class="session-info">
            <div class="session-time">
              <i class="icon-time"></i>
              创建时间: {{ formatDate(session.createdAt) }}
            </div>
            <div class="session-time">
              <i class="icon-update"></i>
              更新时间: {{ formatDate(session.updatedAt) }}
            </div>
          </div>
          
          <div class="session-preview">
            <div v-if="session.messages && session.messages.length > 0" class="last-message">
              <strong>{{ session.messages[session.messages.length - 1].role === 'user' ? '您' : 'AI' }}:</strong>
              {{ truncateText(session.messages[session.messages.length - 1].content, 100) }}
            </div>
            <div v-else class="no-messages">
              暂无消息
            </div>
          </div>
          
          <div class="session-stats">
            <span class="message-count">
              <i class="icon-message"></i>
              {{ session.messages ? session.messages.length : 0 }} 条消息
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { session } from '@/api';
import { auth } from '@/api';

const router = useRouter();
const sessions = ref([]);
const isLoading = ref(false);
const error = ref('');

// 获取当前用户信息
const getCurrentUser = () => {
  const userInfo = auth.getUserInfo();
  if (!userInfo) {
    throw new Error('用户未登录');
  }
  return userInfo;
};

// 加载会话列表
const loadSessions = async () => {
  try {
    isLoading.value = true;
    error.value = '';
    
    const userInfo = getCurrentUser();
    console.log('当前用户信息:', userInfo); // 添加调试信息
    
    if (!userInfo || !userInfo.id) {
      throw new Error('用户信息无效');
    }
    
    console.log('正在调用 session.listSessions，用户ID:', userInfo.id);
    const response = await session.listSessions(userInfo.id);
    console.log('会话列表响应:', response); // 添加调试信息
    console.log('响应类型:', typeof response);
    console.log('是否为数组:', Array.isArray(response));
    
    // 确保response是数组
    if (Array.isArray(response)) {
      sessions.value = response;
    } else if (response && Array.isArray(response.data)) {
      sessions.value = response.data;
    } else if (response && response.sessions && Array.isArray(response.sessions)) {
      sessions.value = response.sessions;
    } else {
      console.warn('响应不是预期的数组格式，设置为空数组');
      sessions.value = [];
    }
    
    console.log('处理后的会话列表:', sessions.value); // 添加调试信息
    console.log('会话数量:', sessions.value.length);
  } catch (err) {
    console.error('获取会话列表失败:', err);
    console.error('错误详情:', {
      message: err.message,
      response: err.response,
      status: err.response?.status,
      data: err.response?.data
    });
    
    if (err.response?.status === 401) {
      error.value = '未授权访问，请重新登录';
      // 清除登录信息
      auth.logout();
      router.push('/login');
    } else if (err.response?.status === 404) {
      error.value = '接口不存在，请检查后端服务';
    } else if (err.response?.status >= 500) {
      error.value = '服务器错误，请稍后重试';
    } else if (err.code === 'ERR_NETWORK') {
      error.value = '网络连接失败，请检查后端服务是否启动';
    } else {
      error.value = err.message || '获取会话列表失败，请稍后重试';
    }
  } finally {
    isLoading.value = false;
  }
};

// 创建新会话
const createNewSession = async () => {
  try {
    isLoading.value = true;
    const userInfo = getCurrentUser();
    
    const response = await session.startSession({ userId: userInfo.id });
    
    if (response && response.sessionId) {
      // 创建成功后跳转到聊天页面
      router.push({
        path: '/chat',
        query: { sessionId: response.sessionId }
      });
    } else {
      throw new Error('创建会话失败');
    }
  } catch (err) {
    console.error('创建会话失败:', err);
    error.value = err.message || '创建会话失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 打开会话
const openSession = (sessionInfo) => {
  router.push({
    path: '/chat',
    query: { sessionId: sessionInfo.sessionId }
  });
};

// 删除会话
const deleteSession = async (sessionId) => {
  if (!confirm('确定要删除这个会话吗？此操作不可恢复。')) {
    return;
  }
  
  try {
    isLoading.value = true;
    await session.deleteSession(sessionId);
    // 重新加载会话列表
    await loadSessions();
  } catch (err) {
    console.error('删除会话失败:', err);
    error.value = err.message || '删除会话失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 获取会话标题
const getSessionTitle = (session) => {
  if (session.messages && session.messages.length > 0) {
    // 使用第一条用户消息作为标题
    const firstUserMessage = session.messages.find(msg => msg.role === 'user');
    if (firstUserMessage) {
      return truncateText(firstUserMessage.content, 30);
    }
  }
  return `会话 ${session.sessionId}`;
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '';
  
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  
  if (diffDays === 0) {
    return '今天 ' + date.toLocaleTimeString('zh-CN', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  } else if (diffDays === 1) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  } else if (diffDays < 7) {
    return `${diffDays}天前`;
  } else {
    return date.toLocaleDateString('zh-CN');
  }
};

// 截断文本
const truncateText = (text, maxLength) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

onMounted(() => {
  console.log('SessionsView 组件挂载'); // 添加调试信息
  
  // 检查登录状态
  if (!auth.isLoggedIn()) {
    console.log('用户未登录，跳转到登录页'); // 添加调试信息
    router.push('/login');
    return;
  }
  
  // 检查用户信息
  const userInfo = auth.getUserInfo();
  console.log('当前用户信息:', userInfo); // 添加调试信息
  
  if (!userInfo) {
    console.log('无法获取用户信息，跳转到登录页'); // 添加调试信息
    router.push('/login');
    return;
  }
  
  // 加载会话列表
  console.log('开始加载会话列表'); // 添加调试信息
  loadSessions();
});
</script>

<style scoped>
.sessions-view {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.sessions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.sessions-header h1 {
  color: #333;
  margin: 0;
  font-size: 28px;
  font-weight: 600;
}

.new-session-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.new-session-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.new-session-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.icon-plus::before {
  content: '➕';
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  text-align: center;
  padding: 40px 20px;
  color: #e74c3c;
  background: #fdf2f2;
  border-radius: 12px;
  margin: 20px 0;
}

.retry-btn {
  margin-top: 16px;
  padding: 8px 20px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.retry-btn:hover {
  background: #c0392b;
}

.sessions-container {
  min-height: 400px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #666;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-state h3 {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 24px;
  font-weight: 500;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
  color: #888;
}

.sessions-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.session-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.session-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  border-color: #667eea;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.session-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  flex: 1;
  margin-right: 12px;
}

.session-actions {
  display: flex;
  gap: 8px;
}

.delete-btn {
  padding: 6px;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: #999;
  transition: all 0.3s ease;
  font-size: 16px;
}

.delete-btn:hover {
  background: #fee;
  color: #e74c3c;
}

.icon-delete::before {
  content: '🗑️';
}

.session-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.session-time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.icon-time::before {
  content: '🕒';
}

.icon-update::before {
  content: '🔄';
}

.session-preview {
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.last-message {
  font-size: 14px;
  line-height: 1.5;
  color: #555;
}

.last-message strong {
  color: #333;
}

.no-messages {
  font-size: 14px;
  color: #999;
  font-style: italic;
}

.session-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.message-count {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
}

.icon-message::before {
  content: '💬';
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sessions-view {
    padding: 16px;
  }
  
  .sessions-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .sessions-header h1 {
    font-size: 24px;
    text-align: center;
  }
  
  .sessions-list {
    grid-template-columns: 1fr;
  }
  
  .session-card {
    padding: 20px;
  }
  
  .session-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .session-actions {
    align-self: flex-end;
  }
}
</style>
