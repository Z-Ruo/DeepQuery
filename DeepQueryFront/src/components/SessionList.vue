<template>
  <div class="session-list" v-if="!isCollapsed">
    <div 
      v-for="session in sortedSessions" 
      :key="session.sessionId"
      class="session-item"
      @click="handleSessionClick(session)"
    >
      <div class="session-content">
        <div class="question-preview">
          {{ getQuestionPreview(session) }}
        </div>
        <div class="session-info">
          <span class="timestamp">{{ formatTime(getFirstMessageTimestamp(session)) }}</span>
          <span class="message-count">{{ getMessageCount(session) }}条对话</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SessionList',
  props: {
    sessions: {
      type: Array,
      required: true,
      default: () => []
    },
    isCollapsed: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    sortedSessions() {
      return [...this.sessions].sort((a, b) => {
        const timeA = this.getFirstMessageTimestamp(a) || 0;
        const timeB = this.getFirstMessageTimestamp(b) || 0;
        return new Date(timeB) - new Date(timeA);
      });
    }
  },
  methods: {
    handleSessionClick(session) {
      if (session) {
        this.$emit('select-session', session);
      }
    },
    getQuestionPreview(session) {
      return session?.historyList?.[0]?.question || '无标题对话';
    },
    getFirstMessageTimestamp(session) {
      return session?.historyList?.[0]?.timestamp || null;
    },
    getMessageCount(session) {
      return session?.historyList?.length || 0;
    },
    formatTime(timestamp) {
      if (!timestamp) return '';
      
      try {
        const date = new Date(timestamp);
        const today = new Date();
        const yesterday = new Date(today);
        yesterday.setDate(yesterday.getDate() - 1);

        if (date.toDateString() === today.toDateString()) {
          return date.toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit'
          });
        } else if (date.toDateString() === yesterday.toDateString()) {
          return '昨天 ' + date.toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit'
          });
        } else {
          return date.toLocaleString('zh-CN', {
            month: 'numeric',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          });
        }
      } catch (error) {
        console.error('时间格式化错误:', error);
        return '';
      }
    }
  }
}
</script>

<style scoped>
.session-list {
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}

.session-item {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.question-preview {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.session-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.timestamp {
  color: #909399;
}

.message-count {
  color: #409EFF;
}
</style> 