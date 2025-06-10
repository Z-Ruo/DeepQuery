// 认证相关的API调用
import axios from 'axios';

/**
 * 用户登录
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @returns {Promise} - 返回包含token的Promise
 */
export const login = async (username, password) => {
  try {
    // 获取用户IP地址 (简化处理，实际可能需要其他方式获取)
    const ip = window.location.hostname || '127.0.0.1';
    
    // 使用全局axios实例
    const response = await axios.post('/v1/auth/login', {
      username,
      password,
      ip
    });
    console.log('登录响应:', response.data);
    // 如果登录成功，保存token和用户名到localStorage
    if (response.data && response.data.status === 200) {
      localStorage.setItem('token', response.data.token); // 确保key为'token'，如果拦截器使用'token'
      localStorage.setItem('username', username);
    }
    
    return response.data;
  } catch (error) {
    console.error('登录失败:', error);
    throw error;
  }
};

/**
 * 用户注册
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @param {string} phone - 手机号
 * @returns {Promise} - 返回注册结果的Promise
 */
export const register = async (username, password, phone) => {
  try {
    // 使用全局axios实例
    const response = await axios.post('/v1/auth/register', {
      username,
      password,
      phone
    });
    
    return response.data;
  } catch (error) {
    console.error('注册失败:', error);
    throw error;
  }
};

/**
 * 检查用户是否已登录
 * @returns {boolean} - 返回用户是否已登录
 */
export const isLoggedIn = () => {
  return !!localStorage.getItem('token'); // 确保key为'token'
};

/**
 * 获取当前用户名
 * @returns {string|null} - 返回用户名或null
 */
export const getCurrentUsername = () => {
  return localStorage.getItem('username');
};

/**
 * 用户登出
 */
export const logout = () => {
  localStorage.removeItem('token'); // 确保key为'token'
  localStorage.removeItem('username');
  // 可选：通知服务器端会话失效，如果后端有相应接口
  // return axios.post('/v1/auth/logout'); 
};

// 默认导出所有认证相关的函数，以便在 api/index.js 中统一管理
export default {
  login,
  register,
  isLoggedIn,
  getCurrentUsername,
  logout
};