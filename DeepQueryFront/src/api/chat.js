// 聊天相关的API调用
import axios from 'axios';

/**
 * 获取聊天完成结果（非流式）
 * @param {string} model - 使用的模型名称 (ollama或zhipu)
 * @param {Array} messages - 消息历史记录
 * @param {Object} options - 额外的选项参数
 * @returns {Promise} - 返回聊天完成结果的Promise
 */
export const getChatCompletions = async (model, messages, options = {}) => {
  try {
    // 获取会话ID，默认从localStorage中获取
    const sessionId = options.sessionId || parseInt(localStorage.getItem('sessionId'), 10) || null;
    
    // 构建请求对象
    const request = {
      status: model || 'zhipu', // 默认使用ollama
      ollama_model: model === 'ollama' ? (options.modelName || 'qwen2.5:0.5b') : undefined,
      zhipu_model: model === 'zhipu' ? (options.modelName || 'glm-4-flash') : undefined,
      sessionId,
      question: {
        role: 'user',
        content: messages[messages.length - 1].content,
        timestamp: new Date().toISOString()
      },
      history: {
        sessionId,
        messages: messages.slice(0, -1) // 除去最后一个，因为它是当前问题
      }
    };

    if (options.date) {
      request.date = options.date;
    }

    const response = await axios.post('/v1/chat/completions', request); // Use global axios
    return response.data;
  } catch (error) {
    console.error('获取聊天回复失败:', error);
    throw error;
  }
};

/**
 * 获取流式聊天结果
 * @param {string} model - 使用的模型名称 (ollama或zhipu)
 * @param {Array} messages - 消息历史记录
 * @param {Object} options - 额外的选项参数
 * @returns {Promise} - 返回包含response的Promise，可以处理流式数据
 */
export const streamChatCompletions = async (model, messages, options = {}) => {
  const maxRetries = options.maxRetries || 3;
  let retryCount = 0;
  
  while (retryCount < maxRetries) {
    try {
      // 获取会话ID，默认从localStorage中获取
      const sessionId = options.sessionId || parseInt(localStorage.getItem('sessionId'), 10) || null;
      
      // 构建请求对象，与普通请求相同
      const request = {
        status: model || 'ollama', // 默认使用ollama
        ollama_model: model === 'ollama' ? (options.modelName || 'qwen2.5:0.5b') : undefined,
        zhipu_model: model === 'zhipu' ? (options.modelName || 'glm-4-flash') : undefined,
        sessionId,
        question: {
          role: 'user',
          content: messages[messages.length - 1].content,
          timestamp: new Date().toISOString()
        },
        history: {
          sessionId,
          messages: messages.slice(0, -1) // 除去最后一个，因为它是当前问题
        }
      };

      if (options.date) {
        request.date = options.date;
      }

      const token = localStorage.getItem('token');
      const headers = {
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache',
        'Accept': 'text/event-stream',
        'Connection': 'keep-alive'
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      // 创建AbortController用于超时控制
      const controller = new AbortController();
      const timeoutId = setTimeout(() => {
        controller.abort();
      }, options.timeout || 300000); // 默认5分钟超时

      // 使用fetch API以便支持流式处理
      const response = await fetch(`${axios.defaults.baseURL}v1/chat/stream`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(request),
        signal: controller.signal
      });

      clearTimeout(timeoutId);

      // 检查响应状态
      if (!response.ok) {
        if (response.status === 500) {
          throw new Error(`服务器内部错误 (${response.status})，请稍后重试`);
        } else if (response.status === 404) {
          throw new Error(`服务接口未找到 (${response.status})，请联系管理员`);
        } else {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
      }

      // 检查是否是流式响应
      const contentType = response.headers.get('content-type');
      if (!contentType || !contentType.includes('text/event-stream')) {
        console.warn('Response is not event-stream, got:', contentType);
      }
      
      return response;
    } catch (error) {
      retryCount++;
      console.error(`获取流式聊天回复失败 (尝试 ${retryCount}/${maxRetries}):`, error);
      
      // 如果是最后一次重试，直接抛出错误
      if (retryCount >= maxRetries) {
        // 根据错误类型提供更友好的错误信息
        if (error.name === 'AbortError') {
          throw new Error('请求超时，请检查网络连接后重试');
        } else if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
          throw new Error('Network connection unavailable');
        } else {
          throw error;
        }
      }
      
      // 等待一段时间后重试（指数退避）
      await new Promise(resolve => setTimeout(resolve, Math.pow(2, retryCount) * 1000));
    }
  }
};

/**
 * 解析SSE流数据
 * @param {ReadableStreamDefaultReader} reader - 可读流的reader
 * @param {TextDecoder} decoder - 文本解码器
 * @param {function} onChunk - 处理每个解析后的数据块的回调 (chunk: { type: 'content' | 'error', data: any }) => void
 * @returns {Promise<void>} - 当流结束时解析的Promise
 */
export const parseSSEStream = async (reader, decoder, onChunk) => {
  let buffer = '';
  let done = false;
  let consecutiveErrors = 0;
  const maxConsecutiveErrors = 5;

  while (!done) {
    try {
      const { value, done: streamDone } = await reader.read();
      done = streamDone;

      // 重置连续错误计数
      consecutiveErrors = 0;

      if (done) {
        // 处理缓冲区中剩余的任何数据
        if (buffer.trim()) {
          const lines = buffer.split('\n');
          let eventData = null;
          
          for (const line of lines) {
            if (line.startsWith('data:')) {
              eventData = line.substring(5).trim();
              if (eventData === '[DONE]') {
                // SSE流正常结束标记
                break;
              }
              try {
                const jsonData = JSON.parse(eventData);
                console.log('Final buffer parsed JSON data:', jsonData); // 调试日志
                
                // 优先检查ChatStreamResponse格式
                if (jsonData.content !== undefined) {
                  onChunk({ type: 'content', data: { content: jsonData.content } });
                }
                // 检查智谱AI格式
                else if (jsonData.choices && jsonData.choices[0] && jsonData.choices[0].delta && jsonData.choices[0].delta.content !== undefined) {
                  onChunk({ type: 'content', data: { content: jsonData.choices[0].delta.content } });
                }
                // 检查Ollama格式
                else if (jsonData.message && jsonData.message.content !== undefined) {
                  onChunk({ type: 'content', data: { content: jsonData.message.content } });
                }
                else {
                  onChunk({ type: 'content', data: jsonData });
                }
              } catch (e) {
                console.error('Failed to parse final buffer JSON data:', eventData, e);
                // 如果不是JSON，尝试作为纯文本处理
                if (eventData && eventData.trim()) {
                  onChunk({ type: 'content', data: { content: eventData } });
                }
              }
            }
          }
        }
        break;
      }

      if (value) {
        buffer += decoder.decode(value, { stream: true });
        let eolIndex;

        // 处理多个事件在一个块中的情况
        while ((eolIndex = buffer.indexOf('\n\n')) >= 0) {
          const eventBlock = buffer.substring(0, eolIndex);
          buffer = buffer.substring(eolIndex + 2);
          
          // 分析事件块，提取数据
          const lines = eventBlock.split('\n');
          let eventType = null;
          let eventData = null;
          
          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventType = line.substring(6).trim();
            } else if (line.startsWith('data:')) {
              eventData = line.substring(5).trim();
            }
          }
          
          // 处理数据
          if (eventData) {
            if (eventData === '[DONE]') {
              // SSE流正常结束标记
              done = true;
              break;
            }
            
            try {
              const jsonData = JSON.parse(eventData);
              console.log('Parsed JSON data:', jsonData); // 调试日志
              
              // 优先检查ChatStreamResponse格式 {content: "...", done: false}
              if (jsonData.content !== undefined) {
                onChunk({ type: 'content', data: { content: jsonData.content } });
              }
              // 检查智谱AI格式 {choices: [{delta: {content: "..."}}]}
              else if (jsonData.choices && jsonData.choices[0] && jsonData.choices[0].delta && jsonData.choices[0].delta.content !== undefined) {
                onChunk({ type: 'content', data: { content: jsonData.choices[0].delta.content } });
              }
              // 检查Ollama格式 {message: {content: "..."}}
              else if (jsonData.message && jsonData.message.content !== undefined) {
                onChunk({ type: 'content', data: { content: jsonData.message.content } });
              }
              // 其他格式直接传递
              else {
                onChunk({ type: 'content', data: jsonData });
              }
            } catch (e) {
              console.error('Failed to parse JSON data:', eventData, e);
              // 如果JSON解析失败，作为纯文本处理
              if (eventData && eventData.trim()) {
                onChunk({ type: 'content', data: { content: eventData } });
              }
            }
          }
        }
      }
    } catch (error) {
      consecutiveErrors++;
      console.error(`Stream reading error (${consecutiveErrors}/${maxConsecutiveErrors}):`, error);
      
      // 如果是网络错误或连接中断，尝试继续读取
      if (error.name === 'AbortError' || error.message.includes('network') || error.message.includes('fetch')) {
        if (consecutiveErrors >= maxConsecutiveErrors) {
          console.error('Too many consecutive stream errors, stopping');
          onChunk({ type: 'error', data: 'Stream connection failed after multiple retries' });
          done = true;
        } else {
          // 短暂等待后继续尝试
          await new Promise(resolve => setTimeout(resolve, 100 * consecutiveErrors));
          continue;
        }
      } else {
        // 非网络错误，直接停止
        onChunk({ type: 'error', data: 'Stream reading error: ' + error.message });
        done = true;
      }
    }
  }

  // 最终检查缓冲区中是否还有剩余内容
  if (buffer.trim()) {
    const lines = buffer.split('\n');
    for (const line of lines) {
      if (line.startsWith('data:')) {
        const dataContent = line.substring(5).trim();
        if (dataContent && dataContent !== '[DONE]') {
          try {
            const jsonData = JSON.parse(dataContent);
            console.log('Final cleanup parsed JSON data:', jsonData); // 调试日志
            
            // 优先检查ChatStreamResponse格式
            if (jsonData.content !== undefined) {
              onChunk({ type: 'content', data: { content: jsonData.content } });
            }
            // 检查智谱AI格式
            else if (jsonData.choices && jsonData.choices[0] && jsonData.choices[0].delta && jsonData.choices[0].delta.content !== undefined) {
              onChunk({ type: 'content', data: { content: jsonData.choices[0].delta.content } });
            }
            // 检查Ollama格式
            else if (jsonData.message && jsonData.message.content !== undefined) {
              onChunk({ type: 'content', data: { content: jsonData.message.content } });
            }
            else {
              onChunk({ type: 'content', data: jsonData });
            }
          } catch (e) {
            console.error('Failed to parse final cleanup JSON data:', dataContent, e);
            if (dataContent.trim()) {
              onChunk({ type: 'content', data: { content: dataContent } });
            }
          }
        }
      }
    }
  }
};


/**
 * 直接询问模型（简单问答，无历史记录）
 * @param {string} question - 问题内容
 * @returns {Promise} - 返回回答的Promise
 */
export const askModel = async (question) => {
  try {
    // 这个接口可能需要根据后端实现调整
    const response = await axios.post('/v1/chat/ask', { question }); // Use global axios
    return response.data;
  } catch (error) {
    console.error('询问模型失败:', error);
    throw error;
  }
};

/**
 * 取消一个正在进行的流式聊天请求
 * @param {string} requestId - 请求ID（可选，如果后端支持）
 * @returns {Promise} - 返回取消结果的Promise
 */
export const cancelChatStream = async (requestId) => {
  try {
    // 如果后端有取消流式聊天的接口，可以调用该接口
    // 目前简单实现，实际使用时可能需要调整
    const response = await axios.post('/v1/chat/cancel', { requestId }); // Use global axios
    return response.data;
  } catch (error) {
    console.error('取消聊天请求失败:', error);
    throw error;
  }
};

/**
 * 获取模型列表
 * @returns {Promise} - 返回可用模型列表的Promise
 */
export const getAvailableModels = async () => {
  try {
    const response = await axios.get('/v1/chat/models'); // Use global axios
    return response.data;
  } catch (error) {
    console.error('获取模型列表失败:', error);
    throw error;
  }
};

/**
 * 提交聊天反馈（点赞/踩）
 * @param {string} messageId - 消息ID
 * @param {boolean} isLiked - 是否点赞
 * @param {string} feedback - 反馈内容（可选）
 * @returns {Promise} - 返回提交结果的Promise
 */
export const submitFeedback = async (messageId, isLiked, feedback = '') => {
  try {
    const response = await axios.post('/v1/chat/feedback', { // Use global axios
      messageId,
      isLiked,
      feedback
    });
    return response.data;
  } catch (error) {
    console.error('提交反馈失败:', error);
    throw error;
  }
};

/**
 * 获取相关问题建议
 * @param {string} query - 用户当前问题
 * @param {number} count - 需要返回的建议数量
 * @returns {Promise} - 返回相关问题建议的Promise
 */
export const getRelatedQuestions = async (query, count = 3) => {
  try {
    const response = await axios.post('/v1/chat/related-questions', { // Use global axios
      query,
      count
    });
    return response.data;
  } catch (error) {
    console.error('获取相关问题失败:', error);
    throw error;
  }
};

/**
 * 处理流式响应，带有自动重连和错误恢复
 * @param {string} model - 使用的模型名称
 * @param {Array} messages - 消息历史记录
 * @param {Object} options - 额外的选项参数
 * @param {function} onChunk - 处理数据块的回调函数
 * @returns {Promise<void>} - 处理完成的Promise
 */
export const handleStreamResponse = async (model, messages, options = {}, onChunk) => {
  const maxRetries = options.maxRetries || 3;
  let retryCount = 0;
  let accumulatedContent = '';

  while (retryCount < maxRetries) {
    try {
      const response = await streamChatCompletions(model, messages, {
        ...options,
        maxRetries: 3 // 内部重试
      });

      if (!response.body) {
        throw new Error('Response does not have a readable stream');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      await parseSSEStream(reader, decoder, (chunk) => {
        if (chunk.type === 'content') {
          accumulatedContent += (typeof chunk.data === 'string' ? chunk.data : JSON.stringify(chunk.data));
        }
        onChunk(chunk);
      });

      // 如果成功完成，跳出重试循环
      break;

    } catch (error) {
      retryCount++;
      console.error(`Stream handling failed (尝试 ${retryCount}/${maxRetries}):`, error);

      if (retryCount >= maxRetries) {
        // 最后一次重试失败，向用户报告错误
        onChunk({ 
          type: 'error', 
          data: `连接失败，已尝试 ${maxRetries} 次。请检查网络连接并重试。错误信息: ${error.message}` 
        });
        throw error;
      }

      // 通知用户正在重试
      onChunk({ 
        type: 'error', 
        data: `连接中断，正在重试... (${retryCount}/${maxRetries})` 
      });

      // 等待后重试
      await new Promise(resolve => setTimeout(resolve, Math.pow(2, retryCount) * 1000));
    }
  }
};

/**
 * 检查网络连接状态
 * @returns {Promise<boolean>} - 网络是否可用
 */
export const checkNetworkConnection = async () => {
  try {
    // 先检查浏览器网络状态
    if (!navigator.onLine) {
      return false;
    }
    
    // 尝试向服务器发送一个简单的请求来检测连接
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);
    
    const response = await fetch(`${axios.defaults.baseURL}v1/chat/completions`, {
      method: 'OPTIONS', // 使用OPTIONS请求减少数据传输
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
      },
      signal: controller.signal
    });
    
    clearTimeout(timeoutId);
    return response.status !== 0; // 任何HTTP响应都表示连接可用
  } catch (error) {
    console.warn('Network connection check failed:', error);
    return false;
  }
};

/**
 * 带连接检查的流式响应处理
 * @param {string} model - 使用的模型名称
 * @param {Array} messages - 消息历史记录
 * @param {Object} options - 额外的选项参数
 * @param {function} onChunk - 处理数据块的回调函数
 * @returns {Promise<void>} - 处理完成的Promise
 */
export const handleStreamResponseWithConnectionCheck = async (model, messages, options = {}, onChunk) => {
  // 首先检查网络连接
  const isConnected = await checkNetworkConnection();
  if (!isConnected) {
    onChunk({ 
      type: 'error', 
      data: '网络连接不可用，请检查网络设置后重试' 
    });
    throw new Error('Network connection unavailable');
  }

  return handleStreamResponse(model, messages, options, onChunk);
};

export default {
  getChatCompletions,
  streamChatCompletions,
  parseSSEStream, // <--- 确保这里导出了 parseSSEStream
  askModel,
  cancelChatStream,
  getAvailableModels,
  submitFeedback,
  getRelatedQuestions,
  handleStreamResponse,
  checkNetworkConnection,
  handleStreamResponseWithConnectionCheck
};
