// RAG（检索增强生成）相关的API调用
import axios from 'axios';

/**
 * 获取所有知识库列表
 * @returns {Promise} - 返回知识库列表Promise
 */
export const getKnowledgeBases = async () => {
  try {
    const response = await axios.post('/v1/rag/collections/list'); // Use global axios
    return response.data;
  } catch (error) {
    console.error('获取知识库列表失败:', error);
    throw error;
  }
};

/**
 * 创建新的知识库
 * @param {string} name - 知识库名称
 * @param {number} dimension - 向量维度（默认值: 768 - 通常由使用的嵌入模型决定）
 * @returns {Promise} - 返回创建结果的Promise
 */
export const createKnowledgeBase = async (name, dimension = 768) => {
  try {
    const response = await axios.post('/v1/rag/collections', { // Use global axios
      name: name,
      dimension: dimension
    });
    return response.data;
  } catch (error) {
    console.error('创建知识库失败:', error);
    throw error;
  }
};

/**
 * 获取指定知识库中的所有文档
 * @param {string} collectionName - 知识库名称
 * @returns {Promise} - 返回文档列表Promise
 */
export const getCollectionDocuments = async (collectionName) => {
  try {
    const response = await axios.post(`/v1/rag/collections/${collectionName}/documents`); // Use global axios
    return response.data;
  } catch (error) {
    console.error('获取文档列表失败:', error);
    throw error;
  }
};

/**
 * 删除指定的知识库
 * @param {string} name - 知识库名称
 * @returns {Promise} - 返回删除结果的Promise
 */
export const deleteKnowledgeBase = async (name) => {
  try {
    const response = await axios.post(`/v1/rag/collections/delete/${name}`); // Use global axios
    return response.data;
  } catch (error) {
    console.error('删除知识库失败:', error);
    throw error;
  }
};

/**
 * 上传文档到指定知识库
 * @param {File} file - 要上传的文件
 * @param {string} collectionName - 知识库名称
 * @returns {Promise} - 返回上传结果的Promise
 */
export const uploadDocument = async (file, collectionName) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('collectionName', collectionName);

    // Use global axios, remove /api prefix, remove manual auth header
    const response = await axios.post('/v1/rag/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        // Authorization header will be added by the global interceptor
      }
    });
    return response.data;
  } catch (error) {
    console.error('文档上传失败:', error);
    throw error;
  }
};

/**
 * 使用RAG系统回答问题
 * @param {string} query - 用户问题
 * @param {string} collectionName - 知识库名称
 * @param {number} maxResults - 最大检索结果数量
 * @param {number} sessionId - 会话ID（可选）
 * @returns {Promise} - 返回回答内容的Promise
 */
export const askRagQuestion = async (query, collectionName, maxResults = 3, sessionId = null) => {
  try {
    const request = {
      query: query,
      collectionName: collectionName,
      maxResults: maxResults,
      sessionId: sessionId
    };

    const response = await axios.post('/v1/rag/question', request); // Use global axios
    return response.data;
  } catch (error) {
    console.error('RAG问答失败:', error);
    throw error;
  }
};

/**
 * 在知识库中搜索内容
 * @param {Object} searchParams - 搜索参数
 * @returns {Promise} - 返回搜索结果的Promise
 */
export const searchInKnowledgeBase = async (searchParams) => {
  try {
    const response = await axios.post('/v1/rag/search', searchParams); // Use global axios
    return response.data;
  } catch (error) {
    console.error('知识库搜索失败:', error);
    throw error;
  }
};

/**
 * 获取所有上传的文档列表
 * @returns {Promise} - 返回文档列表Promise
 */
export const getAllDocuments = async () => {
  try {
    const response = await axios.post('/v1/rag/documents'); // Use global axios
    return response.data;
  } catch (error) {
    console.error('获取文档列表失败:', error);
    throw error;
  }
};

/**
 * 重命名知识库
 * @param {string} oldName - 知识库原名称
 * @param {string} newName - 知识库新名称
 * @returns {Promise} - 返回重命名结果的Promise
 */
export const renameKnowledgeBase = async (oldName, newName) => {
  try {
    const response = await axios.post('/v1/rag/collections/rename', { // Use global axios
      oldName,
      newName
    });
    return response.data;
  } catch (error) {
    console.error('重命名知识库失败:', error);
    throw error;
  }
};

/**
 * 从知识库中删除文档（根据文档ID）
 * @param {string} collectionName - 知识库名称
 * @param {string} documentId - 文档ID
 * @returns {Promise} - 返回删除结果的Promise
 */
export const deleteDocument = async (collectionName, documentId) => {
  try {
    const response = await axios.post(`/v1/rag/collections/${collectionName}/documents/${documentId}/delete`); // Use global axios
    return response.data;
  } catch (error) {
    console.error('删除文档失败:', error);
    throw error;
  }
};

/**
 * 从知识库中删除文档（根据文件名）
 * @param {string} collectionName - 知识库名称
 * @param {string} fileName - 文件名
 * @returns {Promise} - 返回删除结果的Promise
 */
export const deleteDocumentByFileName = async (collectionName, fileName) => {
  try {
    const response = await axios.post(`/v1/rag/collections/${collectionName}/documents/delete-by-name`, null, {
      params: { fileName }
    }); // Use global axios
    return response.data;
  } catch (error) {
    console.error('根据文件名删除文档失败:', error);
    throw error;
  }
};

/**
 * 获取知识库统计信息
 * @param {string} collectionName - 知识库名称
 * @returns {Promise} - 返回统计信息的Promise
 */
export const getKnowledgeBaseStats = async (collectionName) => {
  try {
    const response = await axios.get(`/v1/rag/collections/${collectionName}/stats`); // Use global axios
    return response.data;
  } catch (error) {
    console.error('获取知识库统计信息失败:', error);
    throw error;
  }
};

/**
 * 批量上传多个文档到知识库
 * @param {Array<File>} files - 文件数组
 * @param {string} collectionName - 知识库名称
 * @param {Function} progressCallback - 进度回调函数，可选
 * @returns {Promise} - 返回上传结果的Promise
 */
export const uploadMultipleDocuments = async (files, collectionName, progressCallback) => {
  try {
    const formData = new FormData();
    formData.append('collectionName', collectionName);
    
    // 添加所有文件到formData
    for (let i = 0; i < files.length; i++) {
      formData.append('files', files[i]);
    }
    
    // 配置上传进度回调
    const config = {
      headers: {
        'Content-Type': 'multipart/form-data',
        // Authorization header will be added by the global interceptor
      }
    };
    
    if (progressCallback) {
      config.onUploadProgress = (progressEvent) => {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        progressCallback(percentCompleted);
      };
    }
    
    // Use global axios, remove /api prefix, remove manual auth header
    const response = await axios.post('/v1/rag/upload-batch', formData, config);
    return response.data;
  } catch (error) {
    console.error('批量上传文档失败:', error);
    throw error;
  }
};

export default {
  getKnowledgeBases,
  createKnowledgeBase,
  getCollectionDocuments,
  deleteKnowledgeBase,
  uploadDocument,
  askRagQuestion,
  searchInKnowledgeBase,
  getAllDocuments,
  renameKnowledgeBase,
  deleteDocument,
  deleteDocumentByFileName,
  getKnowledgeBaseStats,
  uploadMultipleDocuments
};
