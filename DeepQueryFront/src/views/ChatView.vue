<template>
  <div class="chat-view-container">
    <!-- 网络状态指示器 -->
    <div v-if="isOffline" class="network-status offline">
      ⚠️ 网络连接已断开，请检查网络设置
    </div>
    
    <!-- 模型状态指示器 -->
    <div class="model-status">
      <span class="current-model">
        当前模型: {{ currentModel === 'zhipu' ? '智谱 AI' : 'Ollama' }}
        {{ currentModel === 'zhipu' ? '🤖' : '🦙' }}
      </span>
      <span v-if="selectedKnowledgeBase" class="knowledge-base-status">
        | 知识库: {{ selectedKnowledgeBase }} 📚
      </span>
      <span v-else class="knowledge-base-status">
        | 直接对话 💬
      </span>
    </div>
    
    <!-- 主体区域 - 分为左侧知识库面板和右侧对话区域 -->
    <div class="main-content">
      <!-- 左侧知识库选择面板 -->
      <div class="left-sidebar">
        <!-- 知识库选择区域 -->
        <div class="knowledge-base-selector">
          <div class="kb-header">
            <h3>📚 知识库</h3>
            <button @click="refreshKnowledgeBases" class="refresh-button" :disabled="isKnowledgeBasesLoading" title="刷新知识库列表">
              🔄
            </button>
          </div>
          
          <div v-if="isKnowledgeBasesLoading" class="kb-loading">
            <span class="loading-spinner">⏳</span>
            正在加载知识库...
          </div>
          
          <div v-else-if="knowledgeBases.length === 0" class="kb-empty">
            <span class="empty-icon">📚</span>
            <div class="empty-text">
              <p>暂无可用知识库</p>
              <small>请先上传文档到知识库</small>
            </div>
          </div>
          
          <div v-else class="kb-list">
            <div 
              @click="clearKnowledgeBase" 
              class="kb-item"
              :class="{ 'active': !selectedKnowledgeBase }"
            >
              <span class="kb-icon">💬</span>
              <span class="kb-name">直接对话</span>
            </div>
            
            <div 
              v-for="kb in knowledgeBases" 
              :key="kb"
              @click="selectKnowledgeBase(kb)" 
              class="kb-item"
              :class="{ 'active': selectedKnowledgeBase === kb }"
            >
              <span class="kb-icon">📚</span>
              <span class="kb-name">{{ kb }}</span>
            </div>
          </div>
        </div>
        
        <!-- 参考来源面板 -->
        <div v-if="selectedKnowledgeBase && currentSources.length > 0" class="knowledge-sources-panel">
          <div class="sources-header">
            <h3>📖 参考来源 <span class="sources-count">({{ currentSources.length }})</span></h3>
            <button @click="closeSources" class="close-sources-btn" title="关闭参考来源">×</button>
          </div>
          <div class="sources-content" ref="sourcesContent" @scroll="handleSourcesScroll">
            <!-- 滚动提示 -->
            <div v-if="currentSources.length > 2" class="scroll-hint">
              <small>💡 向下滚动查看更多来源</small>
            </div>
            
            <div v-for="(source, index) in currentSources" :key="index" class="source-item">
              <div class="source-header">
                <div class="source-title" :title="source.title">{{ source.title }}</div>
                <div class="source-score" :title="`相关度分数: ${(source.score * 100).toFixed(2)}%`">
                  相关度: {{ (source.score * 100).toFixed(1) }}%
                </div>
              </div>
              <div class="source-text" :title="source.segment">{{ source.segment }}</div>
              
              <!-- 源索引指示器 -->
              <div class="source-index">{{ index + 1 }} / {{ currentSources.length }}</div>
            </div>
            
            <!-- 底部提示 -->
            <div v-if="currentSources.length > 0" class="sources-footer">
              <small>📌 以上文档片段用于生成回答</small>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧对话区域 -->
      <div class="chat-area">
        <div class="chat-messages" ref="chatMessagesContainer">
          <div v-for="(msg, index) in messages" :key="`msg-${index}-${msg.timestamp}`" class="message-row" :class="{ 'user-message-row': msg.role === 'user', 'bot-message-row': msg.role === 'assistant' }">
            <div class="message-bubble" :class="{ 'user-bubble': msg.role === 'user', 'bot-bubble': msg.role === 'assistant' }" 
                 @click="msg.role === 'assistant' && msg.isLoadingChunk ? skipTypewriterEffect() : null">
              <div v-if="msg.role === 'assistant' && msg.isLoadingChunk" class="typing-content">
                <div v-html="formatMessageContent(msg.content)" class="streaming-content"></div>
                <span class="typing-cursor">|</span>
                <div class="skip-hint">点击快速完成</div>
              </div>
              <div v-else v-html="formatMessageContent(msg.content)" class="message-content"></div>
              <div class="message-timestamp">{{ msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString() : '' }}</div>
            </div>
          </div>
          <div v-if="isLoading && messages.length === 0" class="loading-initial-message">
            正在努力思考中...
          </div>
            </div>
        
        <div class="chat-input-area">
          <!-- 模型切换按钮 -->
          <button 
            @click="toggleModel" 
            class="model-toggle-button"
            :title="`当前模型: ${currentModel}，点击切换到 ${currentModel === 'zhipu' ? 'ollama' : 'zhipu'}`"
          >
            {{ currentModel === 'zhipu' ? '🤖' : '🦙' }}
          </button>
          
          <!-- 显示模式切换按钮 -->
          <button 
            @click="enableTypewriterEffect = !enableTypewriterEffect" 
            class="mode-toggle-button"
            :title="enableTypewriterEffect ? '点击切换到快速显示模式' : '点击切换到打字机效果模式'"
          >
            {{ enableTypewriterEffect ? '⚡' : '📝' }}
          </button>
          
          <!-- 知识库选择区域已移至左侧边栏 -->
          
          <!-- 重试按钮 -->
          <button v-if="lastFailedMessage && !isOffline" @click="retryLastMessage" class="retry-button" :disabled="isLoading">
            🔄 重试
          </button>
          
          <textarea 
            v-model="newMessage" 
            @keyup.enter.prevent="handleEnterKey" 
            :placeholder="selectedKnowledgeBase ? `正在使用知识库「${selectedKnowledgeBase}」，请输入您的问题... (Shift + Enter 换行)` : '请输入您的问题... (Shift + Enter 换行)'"
            class="chat-input"
            :disabled="isLoading"
          ></textarea>
          <button @click="sendMessage" class="send-button" :disabled="isLoading || !newMessage.trim() || isOffline">
            {{ isLoading ? '思考中...' : (isOffline ? '离线' : '发送') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue';
import { streamChatCompletions, parseSSEStream, handleStreamResponse, handleStreamResponseWithConnectionCheck } from '../api/chat.js'; // 确保路径正确
import { getKnowledgeBases, askRagQuestion } from '../api/rag.js'; // 导入RAG相关API
import DOMPurify from 'dompurify';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css';

const messages = ref([]);
const newMessage = ref('');
const isLoading = ref(false);
const chatMessagesContainer = ref(null); // Ref for the messages container
const isOffline = ref(!navigator.onLine); // 网络状态
const lastFailedMessage = ref(null); // 记录最后失败的消息
const skipTypewriter = ref(false); // 控制是否跳过打字机效果
const enableTypewriterEffect = ref(false); // 控制是否启用打字机效果（默认关闭以获得最快响应）
const scrollThrottled = ref(false); // 滚动节流
const currentModel = ref('zhipu'); // 当前选择的模型，默认为 zhipu
const knowledgeBases = ref([]); // 知识库列表
const selectedKnowledgeBase = ref(null); // 选中的知识库
const isKnowledgeBasesLoading = ref(false); // 知识库加载状态
const currentSources = ref([]); // 当前RAG响应的参考来源

// Helper to scroll to the bottom of the chat messages with throttling
const scrollToBottom = async () => {
  if (scrollThrottled.value) return;
  
  scrollThrottled.value = true;
  await nextTick();
  
  if (chatMessagesContainer.value) {
    chatMessagesContainer.value.scrollTop = chatMessagesContainer.value.scrollHeight;
  }
  
  // 使用 requestAnimationFrame 来节流滚动
  requestAnimationFrame(() => {
    scrollThrottled.value = false;
  });
};

const formatMessageContent = (content) => {
  if (typeof content !== 'string') {
    return '';
  }
  
  // 配置 marked 选项
  marked.setOptions({
    highlight: function(code, lang) {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return hljs.highlight(code, { language: lang }).value;
        } catch (err) {
          console.error('代码高亮失败:', err);
        }
      }
      return hljs.highlightAuto(code).value;
    },
    breaks: true, // 支持换行
    gfm: true, // 支持 GitHub Flavored Markdown
  });
  
  // Convert Markdown to HTML and sanitize
  const rawHtml = marked.parse(content);
  const sanitizedHtml = DOMPurify.sanitize(rawHtml);
  return sanitizedHtml;
};

// 实时流式内容显示函数：立即显示接收到的内容
const displayContentRealtime = async (newContent) => {
  const lastMessage = messages.value[messages.value.length - 1];
  
  // 如果新内容为空，直接返回
  if (!newContent) return;
  
  // 立即显示：直接添加新内容到消息中（确保最快响应）
  lastMessage.content = lastMessage.content + newContent;
  
  // 立即更新DOM，使用微任务确保最快渲染
  await nextTick();
  
  // 如果启用打字机效果且是单字符，添加少量延迟
  if (enableTypewriterEffect.value && !skipTypewriter.value && newContent.length === 1) {
    await new Promise(resolve => setTimeout(resolve, 8));
  }
  
  // 非阻塞式滚动到底部
  scrollToBottom();
};

// 跳过打字机效果
const skipTypewriterEffect = () => {
  skipTypewriter.value = true;
};

// 切换模型
const toggleModel = () => {
  currentModel.value = currentModel.value === 'zhipu' ? 'ollama' : 'zhipu';
  console.log('切换到模型:', currentModel.value);
};

// 获取知识库列表
const loadKnowledgeBases = async () => {
  try {
    isKnowledgeBasesLoading.value = true;
    const response = await getKnowledgeBases();
    console.log('原始响应:', response); // 调试日志
    
    if (response && response.status === 'success' && response.knowledgeList) {
      knowledgeBases.value = response.knowledgeList;
      console.log('知识库列表加载成功:', knowledgeBases.value);
    } else if (response && response.data && response.data.knowledgeList) {
      // 备用解析方式，如果数据在data字段中
      knowledgeBases.value = response.data.knowledgeList;
      console.log('知识库列表加载成功(data字段):', knowledgeBases.value);
    } else {
      console.warn('知识库列表响应格式不正确:', response);
      knowledgeBases.value = [];
    }
  } catch (error) {
    console.error('加载知识库列表失败:', error);
    knowledgeBases.value = [];
    // 可以在这里添加错误提示
  } finally {
    isKnowledgeBasesLoading.value = false;
  }
};

// 选择知识库
const selectKnowledgeBase = (knowledgeBase) => {
  selectedKnowledgeBase.value = knowledgeBase;
  currentSources.value = []; // 清空当前的sources
  console.log('选择知识库:', knowledgeBase);
};  // 清除知识库选择
const clearKnowledgeBase = () => {
  selectedKnowledgeBase.value = null;
  currentSources.value = []; // 清空当前的sources
  console.log('清除知识库选择');
};

// 刷新知识库列表
const refreshKnowledgeBases = async () => {
  await loadKnowledgeBases();
};

// 关闭sources面板
const closeSources = () => {
  currentSources.value = [];
};

// 滚动到源面板顶部
const scrollSourcesToTop = () => {
  const sourcesContent = document.querySelector('.sources-content');
  if (sourcesContent) {
    sourcesContent.scrollTop = 0;
  }
};

// 处理源面板的滚动事件
const handleSourcesScroll = (event) => {
  const element = event.target;
  const scrollHint = element.querySelector('.scroll-hint');
  
  if (scrollHint && element.scrollTop > 50) {
    scrollHint.style.opacity = '0.5';
  } else if (scrollHint) {
    scrollHint.style.opacity = '1';
  }
};

const sendMessage = async () => {
  const trimmedMessage = newMessage.value.trim();
  if (!trimmedMessage) return;

  // 检查网络状态
  if (isOffline.value) {
    alert('网络连接不可用，请检查网络设置');
    return;
  }

  isLoading.value = true;
  skipTypewriter.value = false; // 重置跳过标志
  const userMessage = {
    role: 'user',
    content: trimmedMessage,
    timestamp: new Date().toISOString()
  };
  messages.value.push(userMessage);
  newMessage.value = ''; // Clear input after sending
  await scrollToBottom();

  // Prepare a placeholder for the bot's response
  const botMessagePlaceholder = {
    role: 'assistant',
    content: '',
    timestamp: new Date().toISOString(),
    isLoadingChunk: true // Custom flag for loading state of this specific message
  };
  messages.value.push(botMessagePlaceholder);
  await scrollToBottom();

  try {
    // 如果选择了知识库，使用RAG问答
    if (selectedKnowledgeBase.value) {
      console.log('使用RAG问答，知识库:', selectedKnowledgeBase.value);
      
      try {
        const ragResponse = await askRagQuestion(
          trimmedMessage, 
          selectedKnowledgeBase.value, 
          3, // maxResults
          parseInt(localStorage.getItem('sessionId'), 10) || null
        );
        
        // 处理RAG响应
        if (ragResponse && ragResponse.answer) {
          messages.value[messages.value.length - 1].content = ragResponse.answer;
          messages.value[messages.value.length - 1].isLoadingChunk = false;
          messages.value[messages.value.length - 1].timestamp = new Date().toISOString();
          
          // 将sources存储到左侧面板并显示知识库来源提示
          if (ragResponse.sources && ragResponse.sources.length > 0) {
            currentSources.value = ragResponse.sources;
            console.log(`RAG问答完成，找到${ragResponse.sources.length}个相关文档片段`);
            
            // 延迟滚动源面板到顶部，确保DOM已更新
            await nextTick();
            setTimeout(() => {
              scrollSourcesToTop();
            }, 100);
          } else {
            console.log('RAG问答完成，但未找到相关文档片段');
          }
          
          await scrollToBottom();
        } else {
          throw new Error('RAG响应格式错误');
        }
      } catch (ragError) {
        console.error('RAG问答失败，回退到普通聊天:', ragError);
        // RAG失败时回退到普通聊天，并清空sources
        currentSources.value = [];
        await performNormalChat(trimmedMessage);
      }
    } else {
      // 普通聊天，清空sources
      currentSources.value = [];
      await performNormalChat(trimmedMessage);
    }

  } catch (error) {
    console.error("发送消息失败:", error);
    
    // 记录失败的消息以便重试
    lastFailedMessage.value = trimmedMessage;
    
    // 提供更详细的错误信息
    let errorMessage = "抱歉，回复出错了";
    if (error.message.includes('Network connection unavailable')) {
      errorMessage = "网络连接不可用，请检查网络设置后重试";
    } else if (error.message.includes('timeout')) {
      errorMessage = "请求超时，请稍后重试";
    } else if (error.message.includes('500')) {
      errorMessage = "服务器内部错误，请稍后重试";
    } else if (error.message.includes('404')) {
      errorMessage = "服务接口未找到，请联系管理员";
    } else if (error.message) {
      errorMessage = `连接错误: ${error.message}`;
    }
    
    messages.value[messages.value.length - 1].content = `⚠️ ${errorMessage}`;
    messages.value[messages.value.length - 1].isLoadingChunk = false;
    messages.value[messages.value.length - 1].hasError = true; // 标记为错误状态
  } finally {
    isLoading.value = false;
  }
};

// 执行普通聊天的函数
const performNormalChat = async (trimmedMessage) => {
  const currentMessages = messages.value.slice(0, -1).map(msg => ({ // Exclude placeholder for API call
    role: msg.role,
    content: msg.content
  }));

  let accumulatedContent = '';
  messages.value[messages.value.length - 1].isLoadingChunk = true;

  // 使用带连接检查的处理函数，启用真正的流式显示
  await handleStreamResponseWithConnectionCheck(currentModel.value, currentMessages, { 
    maxRetries: 3,
    timeout: 60000 // 减少到1分钟超时
  }, async (chunk) => {
    console.log('Received chunk:', chunk); // 调试日志
    
    if (chunk.type === 'content') {
      // 处理内容数据
      let contentToAdd = '';
      if (typeof chunk.data === 'object') {
        console.log('Processing object chunk.data:', chunk.data); // 调试日志
        
        if (chunk.data.content !== undefined) {
          contentToAdd = chunk.data.content;
          console.log('Extracted content from chunk.data.content:', contentToAdd); // 调试日志
        } else if (chunk.data.delta && chunk.data.delta.content) {
          // 处理可能的delta格式
          contentToAdd = chunk.data.delta.content;
          console.log('Extracted content from chunk.data.delta.content:', contentToAdd); // 调试日志
        } else if (chunk.data.choices && chunk.data.choices[0] && chunk.data.choices[0].delta) {
          // 处理OpenAI格式的流数据
          contentToAdd = chunk.data.choices[0].delta.content || '';
          console.log('Extracted content from OpenAI format:', contentToAdd); // 调试日志
        } else {
          // 如果是其他对象格式，尝试转换为字符串（但排除原始事件数据）
          const dataStr = JSON.stringify(chunk.data);
          console.log('Object chunk.data as string:', dataStr); // 调试日志
          if (!dataStr.includes('"done":') && !dataStr.includes('event:')) {
            contentToAdd = dataStr;
          }
        }
      } else if (typeof chunk.data === 'string') {
        console.log('Processing string chunk.data:', chunk.data); // 调试日志
        // 如果是字符串，确保不是原始的事件数据
        if (!chunk.data.includes('event:') && !chunk.data.includes('"done":')) {
          contentToAdd = chunk.data;
        }
      }
      
      console.log('Final contentToAdd:', contentToAdd); // 调试日志
      
      if (contentToAdd) {
        // 实现真正的实时流式显示：立即显示接收到的内容，无需等待
        try {
          await displayContentRealtime(contentToAdd);
          accumulatedContent += contentToAdd;
        } catch (renderError) {
          console.error('渲染内容时出错:', renderError);
          // 即使渲染出错，也要继续累积内容
          accumulatedContent += contentToAdd;
        }
      }
    } else if (chunk.type === 'error') {
      console.error("Stream error chunk:", chunk.data);
      // 显示更友好的错误信息
      const errorMessage = typeof chunk.data === 'string' ? chunk.data : '连接出现问题，请稍后重试';
      messages.value[messages.value.length - 1].content = accumulatedContent + `\n\n⚠️ ${errorMessage}`;
      messages.value[messages.value.length - 1].isLoadingChunk = false;
    }
  });

  // 流处理完成
  messages.value[messages.value.length - 1].isLoadingChunk = false;
  messages.value[messages.value.length - 1].timestamp = new Date().toISOString();
  skipTypewriter.value = false; // 重置跳过标志
  scrollToBottom();
};

const handleEnterKey = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault(); // Prevent default Enter behavior (new line)
    sendMessage();
  }
};

// 重试最后失败的消息
const retryLastMessage = async () => {
  if (lastFailedMessage.value && !isLoading.value && !isOffline.value) {
    newMessage.value = lastFailedMessage.value;
    lastFailedMessage.value = null;
    await sendMessage();
  }
};

// Load initial messages or perform other setup if needed
onMounted(() => {
  // 示例：添加欢迎消息来测试渲染
  messages.value = [{
    role: 'assistant', 
    content: '你好！我是智能助手，有什么可以帮助你的吗？\n\n我可以处理各种问题，包括：\n- **编程相关问题**\n- 学术研究\n- 日常咨询\n\n**功能说明：**\n- 🤖/🦙 按钮：切换 智谱AI/Ollama 模型\n- ⚡/📝 按钮：切换快速显示/打字机效果\n- 📚 按钮：选择知识库进行RAG问答\n\n请随时提问！', 
    timestamp: new Date().toISOString()
  }];
  scrollToBottom();
  
  // 加载知识库列表
  loadKnowledgeBases();
  
  // 监听网络状态变化
  const handleOnline = () => {
    isOffline.value = false;
    console.log('网络连接已恢复');
    
    // 如果有失败的消息，提示用户可以重试
    if (lastFailedMessage.value) {
      // 可以在这里显示一个临时提示
      console.log('网络已恢复，您可以重试上一条消息');
    }
  };
  
  const handleOffline = () => {
    isOffline.value = true;
    console.log('网络连接已断开');
  };
  
  window.addEventListener('online', handleOnline);
  window.addEventListener('offline', handleOffline);
  
  // 清理事件监听器
  return () => {
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
  };
});

</script>

<style scoped>
.chat-view-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px); /* Adjust based on NavBar and other elements */
  max-width: 1200px; /* 增加最大宽度以容纳左侧面板 */
  margin: 20px auto;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  overflow: hidden;
  background-color: #f9f9f9;
}

/* 主内容区域布局 */
.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 左侧边栏 */
.left-sidebar {
  width: 350px;
  background-color: #ffffff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 知识库选择区域 */
.kb-header {
  padding: 15px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.refresh-button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 5px;
  border-radius: 4px;
  color: #666;
  transition: all 0.2s;
}

.refresh-button:hover {
  background-color: #e9ecef;
  color: #333;
}

.refresh-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 知识库列表 */
.kb-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 15px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.kb-item:hover {
  background-color: #f8f9fa;
  border-color: #e9ecef;
}

.kb-item.active {
  background-color: #e3f2fd;
  border-color: #2196F3;
  color: #1976d2;
}

.kb-icon {
  font-size: 1.2rem;
  flex-shrink: 0;
}

.kb-name {
  font-size: 0.9rem;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #666;
  font-style: italic;
  gap: 10px;
}

.loading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.kb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #999;
  text-align: center;
}

.empty-icon {
  font-size: 2rem;
  margin-bottom: 10px;
}

.empty-text p {
  margin: 0 0 5px 0;
  font-weight: 500;
}

.empty-text small {
  color: #bbb;
}

.clear-kb-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 15px;
  margin: 0 10px 10px;
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
  color: #856404;
  transition: all 0.2s;
}

.clear-kb-btn:hover {
  background-color: #ffeaa7;
}

/* 左侧知识库来源面板 */
.knowledge-sources-panel {
  width: 350px;
  max-width: 350px;
  background-color: #ffffff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 70vh; /* 限制最大高度，确保在视窗内 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  margin-bottom: 15px;
}

.sources-header {
  padding: 15px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 8px 8px 0 0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.sources-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-sources-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-sources-btn:hover {
  background-color: #e9ecef;
  color: #333;
  transform: scale(1.1);
}

.sources-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 15px;
  min-height: 0; /* 确保flex子元素可以缩小 */
  /* 自定义滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: #007bff #f1f1f1;
}

/* Webkit浏览器的滚动条样式 */
.sources-content::-webkit-scrollbar {
  width: 8px;
}

.sources-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.sources-content::-webkit-scrollbar-thumb {
  background: #007bff;
  border-radius: 4px;
  transition: background 0.2s ease;
}

.sources-content::-webkit-scrollbar-thumb:hover {
  background: #0056b3;
}

/* 滚动提示 */
.sources-content::before {
  content: "";
  position: sticky;
  top: -15px;
  height: 0;
  background: linear-gradient(to bottom, rgba(248, 249, 250, 0.9), transparent);
  z-index: 5;
  pointer-events: none;
}

.sources-content::after {
  content: "";
  position: sticky;
  bottom: -15px;
  height: 0;
  background: linear-gradient(to top, rgba(248, 249, 250, 0.9), transparent);
  z-index: 5;
  pointer-events: none;
}

.source-item {
  margin-bottom: 15px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #007bff;
  transition: all 0.2s ease;
  position: relative;
}

.source-item:hover {
  background-color: #e3f2fd;
  transform: translateX(2px);
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.15);
}

.source-item:last-child {
  margin-bottom: 0;
}

.source-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
  gap: 10px;
}

.source-title {
  font-weight: 600;
  color: #333;
  font-size: 0.9rem;
  flex: 1;
  min-width: 0; /* 允许文本截断 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.source-score {
  font-size: 0.8rem;
  color: #666;
  background-color: #e9ecef;
  padding: 4px 8px;
  border-radius: 12px;
  margin-left: 10px;
  white-space: nowrap;
  min-width: fit-content;
  font-weight: 500;
}

.source-text {
  font-size: 0.85rem;
  line-height: 1.5;
  color: #555;
  text-align: justify;
  word-wrap: break-word;
  overflow-wrap: break-word;
  max-height: 150px; /* 限制每个源文本的最大高度 */
  overflow-y: auto;
  padding-right: 5px;
}

/* 源文本的滚动条样式 */
.source-text::-webkit-scrollbar {
  width: 4px;
}

.source-text::-webkit-scrollbar-track {
  background: transparent;
}

.source-text::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 2px;
}

.source-text::-webkit-scrollbar-thumb:hover {
  background: #999;
}

/* 右侧对话区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #f9f9f9;
}

.network-status {
  padding: 8px 15px;
  text-align: center;
  font-size: 0.9rem;
  font-weight: 500;
}

.network-status.offline {
  background-color: #ffeaa7;
  color: #d63031;
  border-bottom: 1px solid #fdcb6e;
}

.model-status {
  padding: 6px 15px;
  text-align: center;
  font-size: 0.85rem;
  background-color: #e9ecef;
  color: #495057;
  border-bottom: 1px solid #dee2e6;
}

.current-model {
  font-weight: 500;
}

.knowledge-base-status {
  font-weight: 400;
  margin-left: 8px;
}

.chat-messages {
  flex-grow: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message-row {
  display: flex;
  max-width: 75%;
}

.user-message-row {
  align-self: flex-end;
  justify-content: flex-end; /* Ensures bubble is on the right */
}

.bot-message-row {
  align-self: flex-start;
  justify-content: flex-start; /* Ensures bubble is on the left */
}

.message-bubble {
  padding: 10px 15px;
  border-radius: 18px;
  word-wrap: break-word;
  max-width: 100%; /* Bubble can take full width of its row */
  position: relative;
}

.user-bubble {
  background-color: #007bff;
  color: white;
  border-bottom-right-radius: 4px;
}

.bot-bubble {
  background-color: #e9ecef;
  color: #333;
  border-bottom-left-radius: 4px;
}

.message-timestamp {
  font-size: 0.7rem;
  color: #999;
  margin-top: 5px;
  text-align: right;
}

.user-bubble .message-timestamp {
  color: #f0f0f0;
}

.chat-input-area {
  display: flex;
  padding: 15px;
  border-top: 1px solid #e0e0e0;
  background-color: #fff;
}

.chat-input {
  flex-grow: 1;
  padding: 10px 15px;
  border: 1px solid #ccc;
  border-radius: 20px;
  margin-right: 10px;
  font-size: 1rem;
  resize: none; /* Prevent manual resizing */
  min-height: 24px; /* Min height for single line */
  max-height: 100px; /* Max height before scrolling */
  overflow-y: auto; /* Scroll if content exceeds max-height */
  line-height: 1.4;
}

.chat-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0,123,255,0.25);
}

.send-button {
  padding: 10px 20px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s;
}

.send-button:hover {
  background-color: #0056b3;
}

.send-button:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.model-toggle-button {
  padding: 8px 12px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 15px;
  cursor: pointer;
  font-size: 1rem;
  margin-right: 10px;
  transition: background-color 0.2s;
  min-width: 40px;
}

.model-toggle-button:hover {
  background-color: #218838;
}

.mode-toggle-button {
  padding: 8px 12px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 15px;
  cursor: pointer;
  font-size: 1rem;
  margin-right: 10px;
  transition: background-color 0.2s;
  min-width: 40px;
}

.mode-toggle-button:hover {
  background-color: #5a6268;
}

.retry-button {
  padding: 8px 15px;
  background-color: #ffc107;
  color: #333;
  border: none;
  border-radius: 15px;
  cursor: pointer;
  font-size: 0.9rem;
  margin-right: 10px;
  transition: background-color 0.2s;
}

.retry-button:hover {
  background-color: #e0a800;
}

.retry-button:disabled {
  background-color: #f0f0f0;
  color: #999;
  cursor: not-allowed;
}

.loading-initial-message {
  align-self: center;
  color: #777;
  font-style: italic;
}

/* Loading dots for streaming chunks */
.loading-dots span {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: currentColor; /* Use bubble's text color */
  margin: 0 2px;
  animation: blink 1.4s infinite both;
}

.loading-dots span:nth-child(1) {
  animation-delay: 0s;
}
.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}
.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

/* 新的打字机效果样式 */
.typing-content {
  position: relative;
  display: inline-block;
  cursor: pointer;
}

.streaming-content {
  will-change: contents; /* 优化流式内容的渲染性能 */
}

.message-content {
  will-change: auto;
}

.typing-cursor {
  display: inline-block;
  animation: cursor-blink 1s infinite;
  font-weight: normal;
  margin-left: 2px;
}

.skip-hint {
  position: absolute;
  bottom: -20px;
  right: 0;
  font-size: 0.7rem;
  color: #999;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
  font-style: italic;
}

.typing-content:hover .skip-hint {
  opacity: 1;
}

.bot-bubble .typing-content:hover {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  padding: 2px 4px;
  margin: -2px -4px;
}

@keyframes cursor-blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

@keyframes blink {
  0%, 80%, 100% {
    opacity: 0;
  }
  40% {
    opacity: 1;
  }
}

/* Improved Markdown generated HTML styling */
.message-bubble :deep(p) {
  margin: 0 0 0.8em 0;
  line-height: 1.6;
}
.message-bubble :deep(p:last-child) {
  margin-bottom: 0;
}

/* Headers */
.message-bubble :deep(h1),
.message-bubble :deep(h2),
.message-bubble :deep(h3),
.message-bubble :deep(h4),
.message-bubble :deep(h5),
.message-bubble :deep(h6) {
  margin: 1em 0 0.5em 0;
  font-weight: 600;
  line-height: 1.4;
}
.message-bubble :deep(h1) { font-size: 1.5em; }
.message-bubble :deep(h2) { font-size: 1.3em; }
.message-bubble :deep(h3) { font-size: 1.1em; }

/* Lists */
.message-bubble :deep(ul),
.message-bubble :deep(ol) {
  margin: 0.8em 0;
  padding-left: 1.5em;
}
.message-bubble :deep(li) {
  margin-bottom: 0.3em;
  line-height: 1.5;
}
.message-bubble :deep(li:last-child) {
  margin-bottom: 0;
}

/* Code styling */
.message-bubble :deep(code) {
  background-color: rgba(175, 184, 193, 0.2);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9em;
  color: #e83e8c;
}

.user-bubble :deep(code) {
  background-color: rgba(255, 255, 255, 0.2);
  color: #ffeb3b;
}

/* Code blocks */
.message-bubble :deep(pre) {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 1em;
  overflow-x: auto;
  margin: 1em 0;
  line-height: 1.4;
}

.user-bubble :deep(pre) {
  background-color: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.message-bubble :deep(pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  color: inherit;
  font-size: 0.9em;
}

/* Blockquotes */
.message-bubble :deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  border-left: 4px solid #dee2e6;
  background-color: rgba(0, 0, 0, 0.02);
  color: #6c757d;
  font-style: italic;
}

.user-bubble :deep(blockquote) {
  border-left-color: rgba(255, 255, 255, 0.5);
  background-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

/* Tables */
.message-bubble :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
  font-size: 0.9em;
}

.message-bubble :deep(th),
.message-bubble :deep(td) {
  border: 1px solid #dee2e6;
  padding: 0.5em;
  text-align: left;
}

.message-bubble :deep(th) {
  background-color: #f8f9fa;
  font-weight: 600;
}

.user-bubble :deep(th) {
  background-color: rgba(255, 255, 255, 0.1);
}

.user-bubble :deep(th),
.user-bubble :deep(td) {
  border-color: rgba(255, 255, 255, 0.3);
}

/* Links */
.message-bubble :deep(a) {
  color: #007bff;
  text-decoration: underline;
}

.user-bubble :deep(a) {
  color: #87ceeb;
}

.message-bubble :deep(a:hover) {
  text-decoration: none;
}

/* Horizontal rules */
.message-bubble :deep(hr) {
  border: none;
  height: 1px;
  background-color: #dee2e6;
  margin: 1.5em 0;
}

.user-bubble :deep(hr) {
  background-color: rgba(255, 255, 255, 0.3);
}

/* Strong and emphasis */
.message-bubble :deep(strong) {
  font-weight: 600;
}

.message-bubble :deep(em) {
  font-style: italic;
}

/* Math expressions (if using KaTeX) */
.message-bubble :deep(.katex) {
  font-size: 1.1em;
}

.message-bubble :deep(.katex-display) {
  margin: 1em 0;
  text-align: center;
}

/* 新增的参考来源面板样式 */
.sources-count {
  font-size: 0.9rem;
  color: #666;
  font-weight: normal;
}

.scroll-hint {
  text-align: center;
  padding: 10px;
  color: #666;
  background-color: #fff3cd;
  border-radius: 6px;
  margin-bottom: 15px;
  border: 1px solid #ffeeba;
}

.scroll-hint small {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.source-index {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: #007bff;
  color: white;
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: 500;
}

.sources-footer {
  text-align: center;
  padding: 15px 10px;
  color: #666;
  border-top: 1px solid #e9ecef;
  margin-top: 15px;
  background-color: #f8f9fa;
  border-radius: 0 0 8px 8px;
}

.sources-footer small {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

/* 优化移动端体验 */
@media (max-width: 768px) {
  .knowledge-sources-panel {
    width: 100%;
    max-width: none;
    max-height: 50vh;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 1000;
    border-radius: 15px 15px 0 0;
    box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.15);
  }
  
  .sources-header {
    border-radius: 15px 15px 0 0;
  }
  
  .source-item {
    margin-bottom: 10px;
  }
  
  .source-text {
    max-height: 100px;
  }
}

/* 源文本展开动画 */
.source-item:hover .source-text {
  max-height: none;
}

/* 滚动条在移动端的优化 */
@media (max-width: 768px) {
  .sources-content::-webkit-scrollbar {
    width: 12px;
  }
  
  .sources-content::-webkit-scrollbar-thumb {
    background: #007bff;
    border-radius: 6px;
  }
}
</style>
