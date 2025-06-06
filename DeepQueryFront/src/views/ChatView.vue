<template>
  <div class="chat-container">
    <div class="chat-sidebar">
      <el-button type="primary" @click="startNewSession" :loading="loading">新建会话</el-button>
      <div class="session-list">
        <el-scrollbar>
          <div v-for="session in sessions" :key="session.id" 
               @click="selectSession(session)"
               :class="['session-item', { active: currentSession?.id === session.id }]">
            {{ formatSessionTitle(session) }}
          </div>
        </el-scrollbar>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-messages" ref="messagesContainer">
        <el-scrollbar>
          <div v-for="message in currentMessages" :key="message.timestamp" 
               :class="['message', message.role]">
            <div class="message-avatar">
              <el-avatar :size="40" :icon="message.role === 'user' ? 'UserFilled' : 'Service'" />
            </div>
            <div class="message-bubble">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-time">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <div class="chat-input">
        <el-input
          v-model="messageInput"
          type="textarea"
          :rows="3"
          placeholder="输入消息..."
          @keyup.enter.exact.prevent="sendMessage"
          :disabled="!currentSession || loading"
        />
        <el-button type="primary" @click="sendMessage" :loading="loading" :disabled="!currentSession">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { chatApi, sessionApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Service, UserFilled } from '@element-plus/icons-vue'

const sessions = ref([])
const currentSession = ref(null)
const currentMessages = ref([])
const messageInput = ref('')
const loading = ref(false)
const messagesContainer = ref(null)

// 格式化会话标题
const formatSessionTitle = (session) => {
  if (session.title) return session.title
  if (session.firstMessage) return session.firstMessage.substring(0, 20) + '...'
  return '新会话'
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleString()
}

// 获取会话列表
const fetchSessions = async () => {
  try {
    loading.value = true
    // 这里应该从登录信息中获取userId
    const userId = localStorage.getItem('userId') || 1
    const response = await sessionApi.listSessions(userId)
    sessions.value = response
    if (sessions.value.length > 0 && !currentSession.value) {
      await selectSession(sessions.value[0])
    }
  } catch (error) {
    ElMessage.error('获取会话列表失败')
  } finally {
    loading.value = false
  }
}

// 选择会话
const selectSession = async (session) => {
  try {
    loading.value = true
    currentSession.value = session
    const messages = await sessionApi.getSessionMessages(session.id)
    currentMessages.value = messages
    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('获取会话消息失败')
  } finally {
    loading.value = false
  }
}

// 开始新会话
const startNewSession = async () => {
  try {
    loading.value = true
    const userId = localStorage.getItem('userId') || 1
    const response = await sessionApi.startSession({ userId })
    sessions.value.unshift(response)
    await selectSession(response)
  } catch (error) {
    ElMessage.error('创建新会话失败')
  } finally {
    loading.value = false
  }
}

// 发送消息
const sendMessage = async () => {
  if (!messageInput.value.trim() || loading.value || !currentSession.value) return

  const userMessage = {
    role: 'user',
    content: messageInput.value,
    timestamp: new Date().toISOString()
  }

  try {
    loading.value = true
    currentMessages.value.push(userMessage)
    scrollToBottom()

    const request = {
      sessionId: currentSession.value.id,
      model: 'qwen2.5:0.5b',
      question: userMessage,
      history: {
        messages: currentMessages.value.slice(-10), // 只发送最近10条消息作为历史记录
        sessionId: currentSession.value.id
      }
    }

    messageInput.value = ''

    // 使用流式响应
    const eventSource = chatApi.sendMessageStream(request)
    let currentResponse = ''

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.done) {
          eventSource.close()
          loading.value = false
          // 保存完整的回复到历史记录
          const modelMessage = {
            role: 'model',
            content: currentResponse,
            timestamp: new Date().toISOString()
          }
          currentMessages.value.push(modelMessage)
          fetchSessions() // 刷新会话列表
        } else {
          currentResponse = data.content || ''
          // 实时更新UI显示
          const lastMessage = currentMessages.value[currentMessages.value.length - 1]
          if (lastMessage?.role === 'model') {
            lastMessage.content = currentResponse
          } else {
            currentMessages.value.push({
              role: 'model',
              content: currentResponse,
              timestamp: new Date().toISOString()
            })
          }
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('SSE错误:', error)
      eventSource.close()
      loading.value = false
      ElMessage.error('接收消息失败')
    }

  } catch (error) {
    loading.value = false
    ElMessage.error('发送消息失败')
    console.error(error)
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      const scrollbar = messagesContainer.value.querySelector('.el-scrollbar__wrap')
      scrollbar.scrollTop = scrollbar.scrollHeight
    }
  })
}

// 组件挂载时获取会话列表
onMounted(() => {
  fetchSessions()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100%;
  gap: 20px;
  background-color: #f5f7fa;
  margin: -20px;
  padding: 20px;
  overflow: hidden;
}

.chat-sidebar {
  width: 250px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background-color: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  overflow: hidden;
}

.session-list {
  flex: 1;
  overflow: auto;
  margin-top: 10px;
  height: calc(100% - 50px); /* 减去新建会话按钮的高度 */
}

.session-item {
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 6px;
  margin-bottom: 5px;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-item.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  height: calc(100% - 120px); /* 减去输入框的高度 */
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  align-items: flex-start;
}

.message.model {
  flex-direction: row;
}

.message.user {
  flex-direction: row-reverse;
}

.message-bubble {
  max-width: 70%;
}

.message-content {
  padding: 12px 16px;
  border-radius: 8px;
  background-color: #f4f4f5;
  line-height: 1.5;
  word-break: break-word;
}

.message.user .message-content {
  background-color: #ecf5ff;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  text-align: right;
}

.chat-input {
  padding: 20px;
  border-top: 1px solid #dcdfe6;
  display: flex;
  gap: 10px;
  background-color: #fff;
}

.chat-input .el-input {
  flex: 1;
}

.el-button {
  align-self: flex-end;
}
</style>
