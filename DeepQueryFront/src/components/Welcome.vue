<template>
  <div class="welcome-container" @click="clearHighlight">
    <img src="@/assets/logo-bg.jpg" alt="徽算系统" class="welcome-logo" />
    <h2>欢迎使用徽算问答</h2>
    <div class="feature-grid">
      <div class="feature-item" @click.stop="highlightArea('question')">
        <img src="@/assets/exam1.png" alt="智能对话" />
        <span>智能对话</span>
        <p>互联网上十万个为什么在线解答</p>
      </div>
      <div class="feature-item" @click.stop="highlightArea('selection')">
        <img src="@/assets/exam2.png" alt="专注数学" />
        <span>专注数学</span>
        <p>可供中小学数学知识库选择</p>
      </div>    </div>
  </div>
</template>

<script>
export default {
  name: 'Welcome',
  data() {
    return {
      activeArea: null
    }
  },
  methods: {
    highlightArea(area) {
      this.activeArea = area;
      // 添加遮罩层类
      document.body.classList.add('highlight-active');
      // 移除之前的高亮类
      document.querySelectorAll('.highlight').forEach(el => {
        el.classList.remove('highlight');
      });
      
      // 添加新的高亮类
      switch(area) {
        case 'question':
          document.querySelector('.question-area')?.classList.add('highlight');
          break;
        case 'selection':
          document.querySelector('.selection-controls')?.classList.add('highlight');
          break;
        case 'learning':
          document.querySelector('.learning-path-icon')?.classList.add('highlight');
          break;
      }
    },
    clearHighlight() {
      this.activeArea = null;
      document.body.classList.remove('highlight-active');
      document.querySelectorAll('.highlight').forEach(el => {
        el.classList.remove('highlight');
      });
    }
  },
  beforeUnmount() {
    // 组件卸载时清除所有效果
    this.clearHighlight();
  }
}
</script>

<style scoped>
.welcome-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  height: 100%;
  position: relative;
  z-index: 1;
}

.welcome-logo {
  width: 120px;
  height: 120px;
  border-radius: 20px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.welcome-container h2 {
  color: #303133;
  margin-bottom: 40px;
  font-size: 24px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  width: 100%;
  max-width: 800px;
}

.feature-item {
  background: #f5f7fa;
  padding: 24px;
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  z-index: 2;
}

.feature-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: #ecf5ff;
}

.feature-item img {
  width: 48px;
  height: 48px;
  border-radius: 10%;
  margin-bottom: 16px;
}

.feature-item span {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.feature-item p {
  font-size: 14px;
  color: #606266;
  margin: 0;
  line-height: 1.6;
}
</style>

<style>
/* 全局样式 */
body.highlight-active::after {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  pointer-events: none;
  z-index: 1;
}

.highlight {
  position: relative;
  z-index: 2;
  animation: highlight-pulse 2s infinite;
}

@keyframes highlight-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(64, 158, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
  }
}

/* 确保高亮元素在遮罩层上方 */
.question-area.highlight,
.selection-controls.highlight,
.learning-path-icon.highlight {
  position: relative;
  z-index: 2;
  background-color: #fff !important;
  border-radius: 8px;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.4);
}

/* 添加过渡效果 */
.question-area,
.selection-controls,
.learning-path-icon {
  transition: all 0.3s ease;
}
</style> 