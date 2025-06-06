import axios from 'axios'
import { ElMessage } from 'element-plus'

const baseURL = 'http://111.229.211.148:8080'

const api = axios.create({
  baseURL,
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 聊天相关接口
export const chatApi = {
  // 发送消息并获取回复
  sendMessage(chatRequest) {
    return api.post('/api/chat/completions', chatRequest)
  },

  // 流式发送消息
  sendMessageStream(chatRequest) {
    const params = new URLSearchParams()
    Object.keys(chatRequest).forEach(key => {
      if (typeof chatRequest[key] === 'object') {
        params.append(key, JSON.stringify(chatRequest[key]))
      } else {
        params.append(key, chatRequest[key])
      }
    })
    
    return new EventSource(`${baseURL}/api/chat/stream?${params.toString()}`)
  },

}

// 会话管理相关接口
export const sessionApi = {
  // 开始新会话
  startSession(request) {
    return api.post('/v1/session/start', request)
  },

  // 获取用户的会话列表
  listSessions(userId) {
    return api.get(`/v1/session/list/${userId}`)
  },

  // 获取用户的上一个会话
  getPreviousSession(userId) {
    return api.get(`/v1/session/previous/${userId}`)
  },

  // 获取会话的消息历史记录
  getSessionMessages(sessionId) {
    return api.get(`/v1/session/messages/${sessionId}`)
  }
}

// 知识库相关接口
export const knowledgeBaseApi = {
  // 获取系统信息
  getInfo() {
    return api.get('/info')
  },

  // 获取分类列表
  getCategories() {
    return api.get('/categories')
  },

  // 获取某个分类下的文章列表
  getCategoryArticles(categoryId, page = 1, size = 10) {
    return api.get(`/articles`, {
      params: {
        categoryId,
        page,
        size
      }
    })
  },

  // 搜索文章
  searchArticles(query, page = 1, size = 10) {
    return api.get(`/articles/search`, {
      params: {
        query,
        page,
        size
      }
    })
  },

  // 获取文章详情
  getArticle(id) {
    return api.get(`/articles/${id}`)
  }
}

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    // 统一处理错误
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)

export default api
