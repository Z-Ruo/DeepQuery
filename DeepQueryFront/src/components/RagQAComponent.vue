<template>
  <div class="rag-qa-component">
    <div class="rag-header">
      <h3>知识库问答</h3>
      <el-select v-model="selectedKnowledgeBase" placeholder="选择知识库" @change="handleKnowledgeBaseChange">
        <el-option
          v-for="kb in knowledgeBases"
          :key="kb"
          :label="kb"
          :value="kb">
        </el-option>
      </el-select>
    </div>
    
    <div class="rag-content">
      <div class="rag-messages" ref="messagesContainer">
        <div v-for="(message, index) in messages" :key="index" 
          :class="['message', message.role === 'user' ? 'user-message' : 'assistant-message']">
          <div class="message-content">
            <div v-html="formatMessage(message.content)"></div>
          </div>
          <div v-if="message.sources && message.sources.length > 0" class="message-sources">
            <div class="sources-title">来源:</div>
            <div v-for="(source, sIndex) in message.sources" :key="sIndex" class="source-item">
              <div class="source-title">{{ source.title }}</div>
              <div class="source-text">{{ truncateText(source.segment, 150) }}</div>
            </div>
          </div>
        </div>
        <div v-if="loading" class="loading-indicator">
          <el-icon class="is-loading"><loading /></el-icon> 正在思考...
        </div>
      </div>
      
      <div class="rag-input">
        <el-input
          v-model="userQuery"
          type="textarea"
          :rows="2"
          placeholder="请输入您的问题..."
          @keyup.enter.ctrl="handleSendQuery"
        />
        <el-button type="primary" @click="handleSendQuery" :disabled="loading">
          <el-icon><el-icon-position /></el-icon>
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick, computed } from 'vue';
import { askRagQuestion, getKnowledgeBases } from '@/api/auth';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css';

export default {
  name: 'RagQAComponent',
  props: {
    sessionId: {
      type: Number,
      default: null
    }
  },
  setup(props) {
    const knowledgeBases = ref([]);
    const selectedKnowledgeBase = ref('');
    const userQuery = ref('');
    const messages = ref([]);
    const loading = ref(false);
    const messagesContainer = ref(null);
    
    const md = new MarkdownIt({
      highlight: function(str, lang) {
        if (lang && hljs.getLanguage(lang)) {
          try {
            return hljs.highlight(str, { language: lang }).value;
          } catch (__) {}
        }
        return ''; // 使用默认的转义
      }
    });

    const formatMessage = (content) => {
      try {
        return md.render(content);
      } catch (e) {
        return content;
      }
    };

    const truncateText = (text, maxLength) => {
      if (text.length <= maxLength) return text;
      return text.substring(0, maxLength) + '...';
    };

    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
      });
    };

    const handleSendQuery = async () => {
      if (!userQuery.value.trim() || !selectedKnowledgeBase.value || loading.value) return;
      
      // 添加用户消息
      messages.value.push({ role: 'user', content: userQuery.value });
      const query = userQuery.value;
      userQuery.value = '';
      scrollToBottom();
      
      // 显示加载状态
      loading.value = true;
      
      try {
        // 调用RAG API
        const response = await askRagQuestion(
          query, 
          selectedKnowledgeBase.value, 
          3, 
          props.sessionId
        );
        
        // 添加助手回复
        messages.value.push({
          role: 'assistant',
          content: response.answer,
          sources: response.sources
        });
        scrollToBottom();
      } catch (error) {
        console.error('RAG问答出错:', error);
        messages.value.push({
          role: 'assistant',
          content: '抱歉，处理您的问题时出现了错误。请稍后再试。'
        });
      } finally {
        loading.value = false;
      }
    };

    const handleKnowledgeBaseChange = () => {
      // 当知识库变更时，可以清空对话或显示提示
      messages.value = [];
    };

    // 获取知识库列表
    const loadKnowledgeBases = async () => {
      try {
        const response = await getKnowledgeBases();
        if (response && response.knowledgeList) {
          knowledgeBases.value = response.knowledgeList;
          if (knowledgeBases.value.length > 0) {
            selectedKnowledgeBase.value = knowledgeBases.value[0];
          }
        }
      } catch (error) {
        console.error('获取知识库列表失败:', error);
      }
    };

    onMounted(() => {
      loadKnowledgeBases();
    });

    return {
      knowledgeBases,
      selectedKnowledgeBase,
      userQuery,
      messages,
      loading,
      messagesContainer,
      handleSendQuery,
      handleKnowledgeBaseChange,
      formatMessage,
      truncateText
    };
  }
};
</script>

<style scoped>
.rag-qa-component {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.rag-header {
  padding: 12px 16px;
  background-color: #f5f5f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
}

.rag-header h3 {
  margin: 0;
  color: #333;
}

.rag-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.rag-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: #fff;
}

.message {
  margin-bottom: 16px;
  max-width: 80%;
}

.user-message {
  margin-left: auto;
  background-color: #e6f7ff;
  border-radius: 10px 10px 0 10px;
  padding: 10px;
}

.assistant-message {
  margin-right: auto;
  background-color: #f5f5f5;
  border-radius: 10px 10px 10px 0;
  padding: 10px;
}

.message-content {
  word-break: break-word;
}

.message-sources {
  margin-top: 8px;
  font-size: 0.9em;
  border-top: 1px solid #e0e0e0;
  padding-top: 8px;
}

.sources-title {
  font-weight: bold;
  margin-bottom: 4px;
  color: #666;
}

.source-item {
  background-color: rgba(0, 0, 0, 0.03);
  padding: 6px;
  border-radius: 4px;
  margin-bottom: 4px;
}

.source-title {
  font-weight: bold;
  font-size: 0.9em;
  margin-bottom: 2px;
}

.source-text {
  color: #666;
  font-size: 0.9em;
}

.rag-input {
  padding: 12px 16px;
  display: flex;
  gap: 8px;
  background-color: #fff;
  border-top: 1px solid #e0e0e0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-style: italic;
  margin: 10px 0;
}

:deep(.el-textarea__inner) {
  resize: none;
}

:deep(.message-content pre) {
  background-color: #f8f8f8;
  border-radius: 4px;
  padding: 10px;
  overflow: auto;
}

:deep(.message-content code) {
  font-family: Consolas, Monaco, 'Andale Mono', monospace;
}
</style>
