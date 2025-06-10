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
    });    console.log('登录响应:', response.data);
    // 如果登录成功，保存token、用户名和用户ID到localStorage
    if (response.data && response.data.status === 200) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('username', username);
      // 保存从后端返回的用户ID
      if (response.data.userId) {
        localStorage.setItem('userId', response.data.userId.toString());
      }
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
 * 获取用户信息（从localStorage获取真实的用户ID）
 * @returns {Object} - 返回包含用户信息的对象
 */
export const getUserInfo = () => {
  const username = getCurrentUsername();
  const userId = localStorage.getItem('userId');
  
  if (!username) {
    return null;
  }
  
  // 如果没有保存的userId，可能是旧版本登录的用户，需要重新登录
  if (!userId) {
    console.warn('未找到用户ID，可能需要重新登录');
    return null;
  }
  
  return {
    id: parseInt(userId, 10), // 转换为数字
    username: username,
    // 其他字段可以根据需要添加
  };
};

/**
 * 用户登出
 */
export const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('userId'); // 清除用户ID
  // 可选：通知服务器端会话失效，如果后端有相应接口
  // return axios.post('/v1/auth/logout'); 
};

// 默认导出所有认证相关的函数，以便在 api/index.js 中统一管理
export default {
  login,
  register,
  isLoggedIn,
  getCurrentUsername,
  getUserInfo,
  logout
};