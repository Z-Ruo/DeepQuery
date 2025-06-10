// 会话管理相关的API调用
import axios from 'axios';

/**
 * 创建新的会话
 * @param {Object} sessionData - 会话数据
 * @returns {Promise} - 返回创建结果的Promise
 */
export const startSession = async (sessionData) => {
  try {
    const response = await axios.post('/v1/session/start', sessionData);
    return response.data;
  } catch (error) {
    console.error('创建会话失败:', error);
    throw error;
  }
};

/**
 * 创建新会话的别名方法，为了保持兼容性
 * @param {Object} sessionData - 会话数据  
 * @returns {Promise} - 返回创建结果的Promise
 */
export const createSession = async (sessionData) => {
  return startSession(sessionData);
};

/**
 * 获取用户的所有会话列表
 * @param {number} userId - 用户ID
 * @returns {Promise} - 返回会话列表的Promise
 */
export const listSessions = async (userId) => {
  try {
    const response = await axios.get(`/v1/session/list/${userId}`);
    return response.data;
  } catch (error) {
    console.error('获取会话列表失败:', error);
    throw error;
  }
};

/**
 * 获取用户的上一个会话
 * @param {number} userId - 用户ID
 * @returns {Promise} - 返回会话数据的Promise
 */
export const getPreviousSession = async (userId) => {
  try {
    const response = await axios.get(`/v1/session/previous/${userId}`);
    return response.data;
  } catch (error) {
    console.error('获取上一个会话失败:', error);
    throw error;
  }
};

/**
 * 获取会话的消息记录（最多前50条）
 * @param {number} sessionId - 会话ID
 * @returns {Promise} - 返回会话消息的Promise
 */
export const getSessionMessages = async (sessionId) => {
  try {
    const response = await axios.get(`/v1/session/messages/${sessionId}`);
    return response.data;
  } catch (error) {
    console.error('获取会话消息失败:', error);
    throw error;
  }
};

/**
 * 更新会话信息
 * @param {number} sessionId - 会话ID
 * @param {Object} sessionData - 会话更新数据
 * @returns {Promise} - 返回更新结果的Promise
 */
export const updateSession = async (sessionId, sessionData) => {
  try {
    const response = await axios.put(`/v1/session/update/${sessionId}`, sessionData);
    return response.data;
  } catch (error) {
    console.error('更新会话失败:', error);
    throw error;
  }
};

/**
 * 删除指定会话
 * @param {number} sessionId - 会话ID
 * @returns {Promise} - 返回删除结果的Promise
 */
export const deleteSession = async (sessionId) => {
  try {
    const response = await axios.delete(`/v1/session/delete/${sessionId}`);
    return response.data;
  } catch (error) {
    console.error('删除会话失败:', error);
    throw error;
  }
};

/**
 * 获取新的会话ID
 * @param {number} userId - 用户ID（可选）
 * @returns {Promise} - 返回新会话ID的Promise
 */
export const getNewSessionId = async (userId = null) => {
  try {
    const data = userId ? { userId } : {};
    const response = await axios.post('/v1/session/new', data);
    if (response.data && response.data.sessionId) {
      localStorage.setItem('sessionId', response.data.sessionId);
    }
    return response.data;
  } catch (error) {
    console.error('获取新会话ID失败:', error);
    throw error;
  }
};

/**
 * 保存会话消息
 * @param {number} sessionId - 会话ID
 * @param {Object} message - 消息对象
 * @returns {Promise} - 返回保存结果的Promise
 */
export const saveSessionMessage = async (sessionId, message) => {
  try {
    const response = await axios.post(`/v1/session/${sessionId}/message`, message);
    return response.data;
  } catch (error) {
    console.error('保存会话消息失败:', error);
    throw error;
  }
};

export default {
  startSession,
  createSession,
  listSessions,
  getPreviousSession,
  getSessionMessages,
  updateSession,
  deleteSession,
  getNewSessionId,
  saveSessionMessage
};
