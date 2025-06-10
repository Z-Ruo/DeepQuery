// API模块统一导出入口
import axios from 'axios';

// 设置全局 Axios 默认值
axios.defaults.baseURL = 'http://localhost:8080/';
axios.defaults.timeout = 30000; // 增加超时时间到30秒
axios.defaults.headers.common['Content-Type'] = 'application/json'; // 设置全局默认请求头

// 全局请求拦截器 - 添加token到请求头
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token'); // 确保这里使用的key与登录时保存的key一致
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 全局响应拦截器 - 处理常见响应错误
axios.interceptors.response.use(
  response => response,
  error => {
    // 处理网络错误
    if (!error.response) {
      console.error('网络连接错误:', error);
      if (error.code === 'ECONNABORTED') {
        error.message = '请求超时，请检查网络连接';
      } else if (error.code === 'ERR_NETWORK') {
        error.message = '网络连接失败，请检查网络设置';
      } else {
        error.message = '网络连接不可用，请稍后重试';
      }
      return Promise.reject(error);
    }

    if (error.response && error.response.status === 401) {
      console.error('认证失败或Token过期，请重新登录');
      // 清除本地存储的认证信息
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      localStorage.removeItem('sessionId'); // 也清除sessionId

      // 派发一个自定义事件，通知应用需要重定向到登录页
      window.dispatchEvent(new CustomEvent('auth-error'));

      // alert('会话已过期或无效，请重新登录。'); // alert可以移除或保留，取决于产品需求
    } else if (error.response && error.response.status >= 500) {
      console.error('服务器错误:', error.response.status);
      error.message = `服务器错误 (${error.response.status})，请稍后重试`;
    }
    return Promise.reject(error);
  }
);

// 导入各个API模块
import authAPI from './auth';
import sessionAPI from './session';
import chatAPI from './chat';
import ragAPI from './rag';

// 导出API命名空间
export const auth = authAPI;
export const session = sessionAPI;
export const chat = chatAPI;
export const rag = ragAPI;

// 默认导出所有API
export default {
  auth,
  session,
  chat,
  rag
};
