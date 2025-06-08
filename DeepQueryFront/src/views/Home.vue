<template>
  <div class="home-container" ref="container"  height="100vh">
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="header-container">
        <img
          v-if="!isCollapsed"
          src="@/assets/logo-2.png"
          alt="徽算系统"
          class="logo"
        />
        <button @click="toggleSidebar" class="toggle-button">
          <img
            :src="isCollapsed ? expandIcon : collapseIcon"
            alt="Toggle Sidebar"
            width="20"
          />
        </button>
      </div>
      <!-- 知识库管理 -->
      <ul id="knowledge-link">
        <li>
          <div class="history-link" @click="showKnowledgeDialog = true">
            <img src="@/assets/zsk.png" alt="知识库管理" width="20" />
            <span v-if="!isCollapsed">知识库管理</span>
          </div>
        </li>
      </ul>
      <ul id="history-link">
        <li>
          <div class="history-link">
            <img src="@/assets/lishijilu.svg" alt="历史记录" width="20" />
            <span v-if="!isCollapsed">历史记录</span>
          </div>
        </li>
      </ul>
      <SessionList
        :sessions="sessions"
        :isCollapsed="isCollapsed"
        @select-session="handleSessionSelect"
      />
    </aside>
    <main class="main-content">
      <div class="model-selection-area">
        <div class="selection-controls">
          <div v-if="!showGradeSelect" class="select-box model-select">
            <div class="current-selection" @click="toggleModelPopup">
              <span>{{ selectedModel }}</span>
            </div>
            <div v-show="showModelPopup" class="selection-popup">
              <div
                v-for="model in models"
                :key="model.value"
                class="selection-option"
                @click="selectModel(model.value)"
              >
                <div class="option-title">{{ model.label }}</div>
              </div>
            </div>
          </div>

          <div v-if="showGradeSelect" class="grade-checkbox-group" id:gradecheckbox>
            <label
              v-for="grade in grades"
              :key="grade.value"
              class="grade-checkbox-option"
            >
              <input
                type="checkbox"
                :value="grade.label"
                v-model="selectedGrades"
                @change="handleGradeChange"
                class="checkbox-input"
              />
              <span class="checkbox-label">{{ grade.label }}</span>
            </label>
          </div>

          <!-- 添加功能模式切换 -->
          <div class="mode-switch">
            <el-radio-group v-model="activeMode" @change="handleModeChange">
              <el-radio-button label="chat">对话模式</el-radio-button>
              <el-radio-button label="rag">知识库问答</el-radio-button>
            </el-radio-group>
          </div>

          <!-- <button @click="toggleGradeSelect" class="toggle-selection-button">
            {{ showGradeSelect ? "切换到模型选择" : "切换到年级选择" }}
          </button> -->
        </div>
        <div class="right-controls">
          <el-tooltip content="个人中心" placement="bottom" effect="dark">
            <div class="profile-icon" @click="showProfile = true">
              <img src="@/assets/gerenzhongxin.svg" alt="个人中心" />
            </div>
          </el-tooltip>
        </div>
      </div>
      <el-dialog v-model="fileDialogVisible" title="知识库文件" width="70%" class="kb-dialog" append-to-body>
        <!-- 文件状态统计 -->
        <div class="status-summary">
          <el-tag type="success"
            >成功入库: {{ fileList.statusCount?.green || 0 }}</el-tag
          >
          <el-tag type="danger"
            >切分失败: {{ fileList.statusCount?.red || 0 }}</el-tag
          >
          <el-tag type="warning"
            >Milvus失败: {{ fileList.statusCount?.yellow || 0 }}</el-tag
          >
          <el-tag type="info"
            >正在入库: {{ fileList.statusCount?.gray || 0 }}</el-tag
          >
        </div>

        <!-- 文件列表表格 -->
        <el-table
          :data="fileList.files"
          style="width: 100%; margin-top: 20px"
          border
          class="kb-table"
        >
          <el-table-column prop="file_name" label="文件名" width="300" />
          <el-table-column prop="file_id" label="文件ID" width="250" />
          <el-table-column prop="status" label="状态" />
          <el-table-column prop="download" label="下载文件">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
              <el-button>下载</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>

      <div class="response-area" ref="responseArea">
        <Welcome v-if="chatHistory.length === 0" />
        <div v-else>
          <div
            v-for="(chat, index) in chatHistory"
            :key="index"
            class="chat-item"
          >
            <div class="question-title">
              <div class="user-info">
                <img src="@/assets/yonghu.svg" alt="访客_huisuan" width="24" />
                <span class="role-text user-text">访客_huisuan</span>
              </div>
              <span
                class="question-content"
                @click="copyToInput(chat.question)"
              >
                {{ chat.question }}
                <div class="tooltip-wrapper">
                  <div class="tooltip-content">
                    <img
                      src="@/assets/fuzhi.svg"
                      alt="复制"
                      class="copy-icon"
                    />
                    <span>复制到输入框</span>
                  </div>
                </div>
              </span>
            </div>
            <div class="answer-content markdown-content">
              <div class="answer-main">
                <div class="bot-info">
                  <img src="@/assets/logo-bg.jpg" alt="徽算" width="24" />
                  <span class="role-text bot-text">徽算</span>
                </div>
                <span class="answer-text" v-html="chat.response"></span>
                <span v-if="chat.isStreaming" class="streaming-indicator">
                  <i class="el-icon-loading"></i>
                  思考中...
                </span>
                <span v-if="chat.isCancelled" class="cancelled-indicator">
                  回答取消
                </span>

                <!-- 添加操作按钮组 -->
                <div class="answer-actions" v-if="!chat.isStreaming">
                  <div
                    class="action-button tooltip-container"
                    :class="{ active: chat.isLiked }"
                    @click="toggleLike(chat)"
                  >
                    <img src="@/assets/yes.svg" alt="点赞" />
                    <div class="action-tooltip">非常不错</div>
                  </div>
                  <div
                    class="action-button tooltip-container"
                    :class="{ active: chat.isDisliked }"
                    @click="toggleDislike(chat)"
                  >
                    <img src="@/assets/no.svg" alt="踩" />
                    <div class="action-tooltip">还待改进</div>
                  </div>
                  <div
                    class="action-button tooltip-container"
                    @click="reAnswer(chat.question)"
                  >
                    <img src="@/assets/return.svg" alt="重新回答" />
                    <div class="action-tooltip">重新回答</div>
                  </div>
                  <div
                    class="action-button tooltip-container"
                    @click="copyText(chat.response)"
                  >
                    <img src="@/assets/fuzhi.svg" alt="复制" />
                    <div class="action-tooltip">复制文本</div>
                  </div>
                </div>
              </div>

              <!-- 相关内容区域 -->
              <div class="related-section">
                <!-- 相关问题 -->
                <div
                  v-if="chat.relatedQuestions.length > 0"
                  class="related-items questions-section"
                >
                  <div class="section-title">
                    <span>相关问题</span>
                  </div>
                  <div class="questions-list">
                    <div
                      v-for="(question, qIndex) in chat.relatedQuestions"
                      :key="'q' + qIndex"
                      class="related-item"
                      @click="handleRelatedQuestionClick(question)"
                    >
                      <el-tooltip :content="question" placement="top">
                        <div class="item-content">
                          <i class="el-icon-question"></i>
                          <span class="item-text">{{ question }}</span>
                        </div>
                      </el-tooltip>
                    </div>
                  </div>
                </div>

                <!-- 相关文献 -->
                <div
                  v-if="showGradeSelect && chat.referenceFiles.length > 0"
                  class="related-items reference-files-section"
                  :class="{ 'show-all': chat.showAllReferences }"
                >
                  <div class="reference-title">
                    <span>相关文献</span>
                    <span
                      v-if="chat.referenceFiles.length > 3"
                      class="view-all"
                      @click="toggleReferenceFiles(chat)"
                    >
                      {{ chat.showAllReferences ? "收起" : "查看全部" }}
                    </span>
                  </div>
                  <div class="reference-list">
                    <div
                      v-for="(file, fIndex) in chat.showAllReferences
                        ? chat.referenceFiles
                        : displayedReferences(chat.referenceFiles)"
                      :key="'f' + fIndex"
                      class="reference-item"
                      @click="handleReferenceFileClick(file)"
                    >
                      <span class="file-name" :title="file.fileName">{{
                        file.fileName
                      }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="uploaded-images" v-if="uploadedImage">
        <div class="image-previews">
          <div class="image-item">
            <img :src="uploadedImage.preview" class="thumbnail" alt="" />
            <el-icon class="remove-icon" @click="removeImage()">
              <Delete />
              <!-- 使用Element Plus的删除图标 -->
            </el-icon>
          </div>
        </div>
      </div>
      <div class="tools-area">
        <div class="tools-left">
          <!-- 新建对话按钮 -->
          <div class="tooltip-container">
            <button @click="createNewChat" class="new-chat-button">
              <i class="el-icon-plus"></i>
              <span>新建对话</span>
            </button>
          </div>

          <!-- 文件上传图标 -->
          <div class="tooltip-container">
            <label class="file-label" @click="triggerFileUpload">
              <img
                src="@/assets/shangchuanwenjian.svg"
                alt="上传文件"
                width="20"
              />
            </label>
            <div class="tooltip">上传pdf文件</div>
          </div>
          <input
            type="file"
            ref="imageInput"
            :key="inputKey"
            accept="image/*"
            @change="handleImageUpload"
            style="display: none"
          />
          <!-- 图片上传图标 -->
          <div class="tooltip-container">
            <label class="image-label" @click="triggerImageUpload">
              <img src="@/assets/shangchuantup.svg" alt="上传图片" width="20" />
            </label>
            <div class="tooltip">上传图片</div>
          </div>
          <input
            type="file"
            @change="handleImageUpload"
            accept="image/*"
            class="image-input"
            ref="imageInput"
            style="display: none"
            id="imageInput"
            name="imageInput"
          />
          <!-- 语音识别图标 -->
          <div class="tooltip-container">
            <button @click="startVoiceRecognition" class="voice-button">
              <img src="@/assets/yuyinshibie.svg" alt="语音识别" width="20" />
            </button>
            <div class="tooltip">语音识别</div>
          </div>
        </div>

        <div class="tools-right">
          <!-- 取消回答按钮 -->
          <div class="tooltip-container" v-if="isStreaming">
            <button @click="cancelAnswer" class="cancel-button">
              取消回答
            </button>
          </div>
        </div>
      </div>
      <div class="question-area">
        <textarea
          v-model="question"
          placeholder="请输入您的问题"
          class="question-input"
          id="questionInput"
          name="questionInput"
          maxlength="1000"
          rows="1"
          @keydown="handleKeyDown"
        ></textarea>
        <button @click="askQuestion" class="ask-button" :disabled="isLoading">
          <img :src="isLoading ? huifuIcon : fasongIcon" alt="提问" />
        </button>
      </div>

      <!-- 添加模式条件渲染 -->
      <div v-if="activeMode === 'chat'" class="chat-container">
        <!-- 原有的对话组件 -->
        <div class="message-container">
          <div class="response-area" ref="responseArea">
            <Welcome v-if="chatHistory.length === 0" />
            <div v-else>
              <div
                v-for="(chat, index) in chatHistory"
                :key="index"
                class="chat-item"
              >
                <div class="question-title">
                  <div class="user-info">
                    <img src="@/assets/yonghu.svg" alt="访客_huisuan" width="24" />
                    <span class="role-text user-text">访客_huisuan</span>
                  </div>
                  <span
                    class="question-content"
                    @click="copyToInput(chat.question)"
                  >
                    {{ chat.question }}
                    <div class="tooltip-wrapper">
                      <div class="tooltip-content">
                        <img
                          src="@/assets/fuzhi.svg"
                          alt="复制"
                          class="copy-icon"
                        />
                        <span>复制到输入框</span>
                      </div>
                    </div>
                  </span>
                </div>
                <div class="answer-content markdown-content">
                  <div class="answer-main">
                    <div class="bot-info">
                      <img src="@/assets/logo-bg.jpg" alt="徽算" width="24" />
                      <span class="role-text bot-text">徽算</span>
                    </div>
                    <span class="answer-text" v-html="chat.response"></span>
                    <span v-if="chat.isStreaming" class="streaming-indicator">
                      <i class="el-icon-loading"></i>
                      思考中...
                    </span>
                    <span v-if="chat.isCancelled" class="cancelled-indicator">
                      回答取消
                    </span>

                    <!-- 添加操作按钮组 -->
                    <div class="answer-actions" v-if="!chat.isStreaming">
                      <div
                        class="action-button tooltip-container"
                        :class="{ active: chat.isLiked }"
                        @click="toggleLike(chat)"
                      >
                        <img src="@/assets/yes.svg" alt="点赞" />
                        <div class="action-tooltip">非常不错</div>
                      </div>
                      <div
                        class="action-button tooltip-container"
                        :class="{ active: chat.isDisliked }"
                        @click="toggleDislike(chat)"
                      >
                        <img src="@/assets/no.svg" alt="踩" />
                        <div class="action-tooltip">还待改进</div>
                      </div>
                      <div
                        class="action-button tooltip-container"
                        @click="reAnswer(chat.question)"
                      >
                        <img src="@/assets/return.svg" alt="重新回答" />
                        <div class="action-tooltip">重新回答</div>
                      </div>
                      <div
                        class="action-button tooltip-container"
                        @click="copyText(chat.response)"
                      >
                        <img src="@/assets/fuzhi.svg" alt="复制" />
                        <div class="action-tooltip">复制文本</div>
                      </div>
                    </div>
                  </div>

                  <!-- 相关内容区域 -->
                  <div class="related-section">
                    <!-- 相关问题 -->
                    <div
                      v-if="chat.relatedQuestions.length > 0"
                      class="related-items questions-section"
                    >
                      <div class="section-title">
                        <span>相关问题</span>
                      </div>
                      <div class="questions-list">
                        <div
                          v-for="(question, qIndex) in chat.relatedQuestions"
                          :key="'q' + qIndex"
                          class="related-item"
                          @click="handleRelatedQuestionClick(question)"
                        >
                          <el-tooltip :content="question" placement="top">
                            <div class="item-content">
                              <i class="el-icon-question"></i>
                              <span class="item-text">{{ question }}</span>
                            </div>
                          </el-tooltip>
                        </div>
                      </div>
                    </div>

                    <!-- 相关文献 -->
                    <div
                      v-if="showGradeSelect && chat.referenceFiles.length > 0"
                      class="related-items reference-files-section"
                      :class="{ 'show-all': chat.showAllReferences }"
                    >
                      <div class="reference-title">
                        <span>相关文献</span>
                        <span
                          v-if="chat.referenceFiles.length > 3"
                          class="view-all"
                          @click="toggleReferenceFiles(chat)"
                        >
                          {{ chat.showAllReferences ? "收起" : "查看全部" }}
                        </span>
                      </div>
                      <div class="reference-list">
                        <div
                          v-for="(file, fIndex) in chat.showAllReferences
                            ? chat.referenceFiles
                            : displayedReferences(chat.referenceFiles)"
                          :key="'f' + fIndex"
                          class="reference-item"
                          @click="handleReferenceFileClick(file)"
                        >
                          <span class="file-name" :title="file.fileName">{{
                            file.fileName
                          }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="uploaded-images" v-if="uploadedImage">
            <div class="image-previews">
              <div class="image-item">
                <img :src="uploadedImage.preview" class="thumbnail" alt="" />
                <el-icon class="remove-icon" @click="removeImage()">
                  <Delete />
                  <!-- 使用Element Plus的删除图标 -->
                </el-icon>
              </div>
            </div>
          </div>
          <div class="tools-area">
            <div class="tools-left">
              <!-- 新建对话按钮 -->
              <div class="tooltip-container">
                <button @click="createNewChat" class="new-chat-button">
                  <i class="el-icon-plus"></i>
                  <span>新建对话</span>
                </button>
              </div>

              <!-- 文件上传图标 -->
              <div class="tooltip-container">
                <label class="file-label" @click="triggerFileUpload">
                  <img
                    src="@/assets/shangchuanwenjian.svg"
                    alt="上传文件"
                    width="20"
                  />
                </label>
                <div class="tooltip">上传pdf文件</div>
              </div>
              <input
                type="file"
                ref="imageInput"
                :key="inputKey"
                accept="image/*"
                @change="handleImageUpload"
                style="display: none"
              />
              <!-- 图片上传图标 -->
              <div class="tooltip-container">
                <label class="image-label" @click="triggerImageUpload">
                  <img src="@/assets/shangchuantup.svg" alt="上传图片" width="20" />
                </label>
                <div class="tooltip">上传图片</div>
              </div>
              <input
                type="file"
                @change="handleImageUpload"
                accept="image/*"
                class="image-input"
                ref="imageInput"
                style="display: none"
                id="imageInput"
                name="imageInput"
              />
              <!-- 语音识别图标 -->
              <div class="tooltip-container">
                <button @click="startVoiceRecognition" class="voice-button">
                  <img src="@/assets/yuyinshibie.svg" alt="语音识别" width="20" />
                </button>
                <div class="tooltip">语音识别</div>
              </div>
            </div>

            <div class="tools-right">
              <!-- 取消回答按钮 -->
              <div class="tooltip-container" v-if="isStreaming">
                <button @click="cancelAnswer" class="cancel-button">
                  取消回答
                </button>
              </div>
            </div>
          </div>
          <div class="question-area">
            <textarea
              v-model="question"
              placeholder="请输入您的问题"
              class="question-input"
              id="questionInput"
              name="questionInput"
              maxlength="1000"
              rows="1"
              @keydown="handleKeyDown"
            ></textarea>
            <button @click="askQuestion" class="ask-button" :disabled="isLoading">
              <img :src="isLoading ? huifuIcon : fasongIcon" alt="提问" />
            </button>
          </div>
        </div>
      </div>
      
      <!-- RAG问答组件 -->
      <div v-else-if="activeMode === 'rag'" class="rag-container">
        <RagQAComponent :sessionId="currentSessionId" />
      </div>
      
      <!-- 添加 ProfilePopup 组件 -->
      <ProfilePopup v-model:visible="showProfile" />

      <!-- 优化后的知识库管理弹窗 -->
      <el-dialog
        v-model="showKnowledgeDialog"
        title=""
        width="420px"
        class="kb-dialog"
        append-to-body
      >
        <div class="kb-popup-header">
          <img src="@/assets/zsk.png" alt="知识库管理" class="kb-header-icon" />
          <h3>知识库管理</h3>
          <el-button
            type="primary"
            size="small"
            @click="showAddDialog = true"
            class="kb-add-btn"
          >
            添加知识库
          </el-button>
        </div>
        <div class="kb-list">
          <div v-for="grade in grades" :key="grade.value" class="kb-list-item">
            <div class="kb-content">
              <i class="el-icon-folder-opened kb-icon"></i>
              <span class="kb-name">{{ grade.label }}</span>
              <div class="kb-actions">
                <el-button link size="small" @click="viewKnowledgeBase(grade.value)">
                  <i class="el-icon-view"></i> 查看
                </el-button>
                <el-button
                  link
                  size="small"
                  @click="deleteKnowledgeBase(grade.value)"
                >
                  <i class="el-icon-delete"></i> 删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-dialog>

      <!-- 添加知识库的弹窗 -->
      <el-dialog v-model="showAddDialog" title="添加知识库" width="420px" class="kb-dialog" append-to-body>
        <el-input v-model="newKbName" placeholder="请输入知识库名称" clearable>
        </el-input>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="showAddDialog = false">取消</el-button>
            <el-button type="primary" @click="createKnowledgeBase">确定</el-button>
          </span>
        </template>
      </el-dialog>

      <!-- QAComponent 组件 -->
      <QAComponent />
    </main>
  </div>
</template>

<script>
import expandIcon from "@/assets/zhankai.svg";
import collapseIcon from "@/assets/shousuo.svg";
import fasongIcon from "@/assets/fasong.svg";
import huifuIcon from "@/assets/huifu.png";
import {
  getChatCompletions,
  // getHistoryRecords,
  // getNewSessionId,
  getReferenceFiles,
  // getRelatedQuestions,
  //getSessionId,
  parseSSE,
} from "../api/auth";
import { ElMessage } from "element-plus";
import { marked } from "marked";
import ProfilePopup from "../components/ProfilePopup.vue";
import Welcome from "../components/Welcome.vue";
import SessionList from "../components/SessionList.vue";
import axios from "axios";
import katex from "katex";
import "katex/dist/katex.min.css";
import { Delete } from "@element-plus/icons-vue";
import QAComponent from "../components/QAComponent.vue";
import RagQAComponent from "../components/RagQAComponent.vue"; // 导入新组件
import "../assets/style/home.css"; // Added external CSS file for Home.vue

export default {
  components: {
    Delete,
    ProfilePopup,
    Welcome,
    SessionList,
    QAComponent,
    RagQAComponent, // 注册RAG组件
  },
  data() {
    return {
      fileDialogVisible: false,
      selectedGrades: [],
      selectedGradeIds: [],
      newKbName: "",
      showAddDialog: false,
      fasongIcon,
      huifuIcon,
      question: "",
      chatHistory: [],
      isCollapsed: false,
      expandIcon,
      collapseIcon,
      selectedModel: "",
      selectedGradeName: "",
      models: [],
      uploadedFile: null,
      uploadedImage: null,
      recognition: null,
      isLoading: false,
      isStreaming: false,
      currentStream: "",
      isCancelled: false,
      selectedGrade: "",
      grades: [],
      showGradeSelect: true,
      currentResponse: "",
      referenceFiles: [],
      showModelPopup: false,
      showGradePopup: false,
      showProfile: false,
      containerRef: null,
      sessions: [],
      questionId: null,
      maxImageSize: 10 * 1024 * 1024,
      inputKey: 0,
      ocrProcessing: false,
      forceRerenderKey: 0,
      pythonOcrUrl: "http://10.10.52.215:8000/ocr",
      fileList: {
        statusCount: {
          green: 0,
          red: 0,
          yellow: 0,
          gray: 0,
        },
        files: [],
      },
      showKnowledgeDialog: false,
      rawResponse: "",
      activeMode: 'chat', // 默认为普通对话模式
    };
  },

  mounted() {
    this.containerRef = this.$refs.container;
    document.addEventListener("click", this.handleOutsideClick);
  },
  updated() {
    this.$nextTick(() => {
      const container = this.$refs.responseArea;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    });
  },
  methods: {
    handleDialogClose() {
      console.log("对话框已关闭");
      // 这里可以添加清理逻辑
      this.searchQuery = "";
      this.currentPage = 1;
      this.fileList = [];
    },
    async createKnowledgeBase() {
      if (!this.newKbName.trim()) {
        this.$message.warning("请输入知识库名称");
        return;
      }

      try {
        const response = await this.$axios.post("/knowledge/create", null, {
          params: { kbName: this.newKbName },
        });
        this.$message.success(response.data);
        this.showAddDialog = false;
        this.newKbName = "";
        // 刷新知识库列表
        await this.fetchModels();
      } catch (error) {
        this.$message.error("创建失败: " + error.message);
      }
    },
    async viewKnowledgeBase(id) {
      console.log(1);
      try {
        const response = await this.$axios.post("/knowledge/files", {
          kb_id: id,
        });

        this.fileList = response.data;
        this.fileDialogVisible = true;
      } catch (error) {
        this.$message.error("获取文件列表失败");
      }
    },
    async deleteKnowledgeBase(id) {
      try {
        await this.$confirm("确定删除该知识库吗？", "提示", {
          type: "warning",
        });
        await this.$axios.delete(`/knowledge/delete`, { data: [id] });
        this.$message.success("删除成功");
        await this.fetchModels();
      } catch (error) {
        if (error !== "cancel") {
          this.$message.error("删除失败");
        }
      }
    },
    handleKeyDown(event) {
      if (event.key === "Enter") {
        if (event.ctrlKey) {
          // Ctrl+Enter: 插入换行
          const start = event.target.selectionStart;
          const end = event.target.selectionEnd;
          const value = event.target.value;
          this.question =
            value.substring(0, start) + "\n" + value.substring(end);
          this.$nextTick(() => {
            event.target.selectionStart = event.target.selectionEnd = start + 1;
          });
        } else {
          // 仅Enter: 提交问题
          event.preventDefault();
          if (this.question.trim() && !this.isLoading) {
            this.askQuestion();
          }
        }
      }
    },
    triggerFileUpload() {
      this.$refs.fileInput.click();
    },
    triggerImageUpload() {
      this.$refs.imageInput.value = null;
      this.$refs.imageInput.click();
    },
    handleImageUpload(event) {
      const file = event.target.files[0];
      if (!file) return;

      // 替换原有图片
      this.uploadedImage = {
        file: file,
        preview: URL.createObjectURL(file),
      };

      // 清空文件输入，允许重复选择同一文件
      event.target.value = "";
    },
    removeImage() {
      if (this.uploadedImage) {
        URL.revokeObjectURL(this.uploadedImage.preview);
        this.uploadedImage = null;
      }
    },
    async processImageWithOCR() {
      try {
        // 确保 uploadedImage 存在
        if (!this.uploadedImage) {
          return null;
        }

        const formData = new FormData();
        formData.append("file", this.uploadedImage.file);
        const response = await axios.post(
          "http://10.10.52.215:8000/ocr",
          formData,
          {
            headers: {
              "Content-Type": "multipart/form-data",
            },
            timeout: 30000,
          }
        );
        this.uploadedImage = null;
        return response.data.text || response.data.result;
      } catch (error) {
        throw new Error(`OCR处理失败: ${error.message}`);
      }
    },
    async askQuestion() {
      this.isCancelled = false;
      this.isStreaming = true;
      const messages = [];
      const questionText = this.question; // 保存当前问题
      this.question = ""; // 立即清空输入框
      const kbInfo = this.selectedGradeIds.map((id, index) => ({
        kb_id: id,
        kb_name: this.selectedGrades[index] || `知识库-${id}`, // 防止名称列表长度不匹配
      }));
      this.isLoading = true;
      let index;
      try {
        const newChat = {
          question: questionText,
          response: "",
          isStreaming: true,
          relatedQuestions: [],
          referenceFiles: [],
          isLiked: false,
          isDisliked: false,
          showAllReferences: false,
          ocrText: "",
        };
        this.chatHistory = [...this.chatHistory, newChat];
        index = this.chatHistory.length - 1;
        this.currentResponse = "";
        this.rawResponse = ""; // 在开始新的回答前重置原始回答
        // 添加最近5条历史记录（问题和回答）
        const recentHistory = this.chatHistory.slice(-5); // 获取最后5条记录
        recentHistory.forEach((chat) => {
          // 添加用户问题
          messages.push({
            role: "user",
            content: chat.question,
          });
          // 添加AI回答
          if (chat.response) {
            messages.push({
              role: "assistant",
              content: chat.response,
            });
          }
        });
        const response = await getChatCompletions(
          this.selectedModel,
          messages,
          kbInfo
        );
        const reader = response.body
          .pipeThrough(new TextDecoderStream("utf-8"))
          .pipeThrough(parseSSE())
          .getReader();

        while (true) {
          const { done, value } = await reader.read();
          if (done || this.isCancelled) break;

          if (value.type === "error") {
            throw new Error(value.error);
          }

          if (value.type === "message") {
            const content = value.data.choices[0].message.content;
            this.currentResponse += content;
            this.rawResponse += content;
          } else if (value.type === "complete") {
            break;
          }
        }

        this.chatHistory[index].isStreaming = false;
      } catch (error) {
        console.error("提问失败:", error);
        if (index !== undefined) {
          this.chatHistory[index].response = "请求出错，请重试";
          this.chatHistory[index].isStreaming = false;
        } else {
          ElMessage.error("提问失败，请检查网络连接");
        }
      } finally {
        this.isLoading = false;
        this.isStreaming = false;
        this.question = "";
        this.currentResponse = "";
        this.rawResponse = ""; // 清空原始回答
        this.uploadedImage = null;
      }
    },
    getStatusTagType(status) {
      const map = {
        green: "success",
        red: "danger",
        yellow: "warning",
        gray: "info",
      };
      return map[status] || "";
    },
    getStatusText(status) {
      const map = {
        green: "成功入库",
        red: "切分失败",
        yellow: "Milvus失败",
        gray: "正在入库",
      };
      return map[status] || status;
    },
    startVoiceRecognition() {
      if (!("webkitSpeechRecognition" in window)) {
        ElMessage.error("抱歉，您的浏览器不支持语音识别。");
        return;
      }

      this.recognition = new webkitSpeechRecognition();
      this.recognition.lang = "zh-CN";
      this.recognition.interimResults = false;
      this.recognition.maxAlternatives = 1;

      this.recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        this.question = transcript;
        ElMessage.success("识别结果: " + transcript);
      };

      this.recognition.onerror = (event) => {
        ElMessage.error("语音识别出错: " + event.error);
      };

      this.recognition.start();
    },
    toggleSidebar() {
      this.isCollapsed = !this.isCollapsed;
    },
    // async toggleGradeSelect() {
    //   this.showGradeSelect = !this.showGradeSelect;
    // },
    handleRelatedQuestionClick(question) {
      this.question = question;
      this.askQuestion();
    },
    async createNewChat() {
      this.chatHistory = [];
      this.question = "";
      this.currentResponse = "";
      await getNewSessionId();

      // 刷新历史记录
      await this.fetchSessions();

      ElMessage.success("已新建对话");
    },
    cancelAnswer() {
      this.isCancelled = true;
      const lastChat = this.chatHistory[this.chatHistory.length - 1];
      if (lastChat) {
        lastChat.isStreaming = false;
        lastChat.isCancelled = true;
      }
      this.isStreaming = false;
    },
    toggleModelPopup() {
      this.showModelPopup = !this.showModelPopup;
      this.showGradePopup = false;
    },
    toggleGradePopup() {
      this.showGradePopup = !this.showGradePopup;
      this.showModelPopup = false;
    },
    selectModel(value) {
      this.selectedModel = value;
      this.showModelPopup = false;
    },
    selectGrade(label) {
      this.selectedGradeName = label;
      const selectedGradeObj = this.grades.find(
        (grade) => grade.label === label
      );
      if (selectedGradeObj) {
        this.selectedGrade = selectedGradeObj.value;
      }
      this.showGradePopup = false;
    },
    handleGradeChange() {
      // 根据选中的标签获取对应的ID
      this.selectedGradeIds = this.grades
        .filter((grade) => this.selectedGrades.includes(grade.label))
        .map((grade) => grade.value);
    },
    handleOutsideClick(e) {
      const modelSelect = document.querySelector(".model-select");
      const gradeSelect = document.querySelector(".grade-select");

      if (modelSelect && gradeSelect) {
        const isClickOutside =
          !modelSelect.contains(e.target) && !gradeSelect.contains(e.target);
        const isClickOnTrigger = e.target.closest(".current-selection");

        if (isClickOutside && !isClickOnTrigger) {
          this.showModelPopup = false;
          this.showGradePopup = false;
        }
      }
    },

    handleSessionSelect(session) {
      // 添加防护检查
      if (!session || !session.historyList) {
        console.error("无效的会话数据");
        return;
      }

      // 清空当前对话
      this.chatHistory = [];

      // 将选中会话的历史记录转换为聊天记录格式
      session.historyList.forEach((history) => {
        if (history && history.question && history.answer) {
          try {
            // 直接处理历史记录中的回答，不需要流式处理
            let processedResponse = history.answer;

            // 如果回答是Markdown格式，则进行渲染
            try {
              const parsedHtml = marked(history.answer);
              processedResponse = this.processLatex(parsedHtml);
            } catch (renderError) {
              console.error("渲染历史记录失败:", renderError);
              // 如果渲染失败，使用原始文本
              processedResponse = history.answer;
            }

            this.chatHistory.push({
              question: history.question,
              response: processedResponse,
              isStreaming: false,
              // 保留历史记录中的相关问题和文献
              relatedQuestions: history.relatedQuestions || [],
              referenceFiles: history.referenceFiles || [],
              isLiked: history.isLiked || false,
              isDisliked: history.isDisliked || false,
              showAllReferences: false,
            });
          } catch (error) {
            console.error("历史记录格式化错误:", error);
            // 如果格式化失败，使用原始文本
            this.chatHistory.push({
              question: history.question,
              response: history.answer,
              isStreaming: false,
              relatedQuestions: history.relatedQuestions || [],
              referenceFiles: history.referenceFiles || [],
              isLiked: history.isLiked || false,
              isDisliked: history.isDisliked || false,
              showAllReferences: false,
            });
          }
        }
      });

      // 更新当前会话ID
      if (session.sessionId) {
        localStorage.setItem("sessionId", session.sessionId);
      }
    },
    displayedReferences(references) {
      // 只显示前3个文献
      return references.slice(0, 3);
    },
    toggleReferenceFiles(chat) {
      chat.showAllReferences = !chat.showAllReferences;
    },
    toggleLike(chat) {
      if (chat.isDisliked) {
        chat.isDisliked = false;
      }
      chat.isLiked = !chat.isLiked;
    },
    toggleDislike(chat) {
      if (chat.isLiked) {
        chat.isLiked = false;
      }
      chat.isDisliked = !chat.isDisliked;
    },
    reAnswer(question) {
      this.question = question;
      this.askQuestion();
    },
    copyText(text) {
      // 移除HTML标签
      const tempDiv = document.createElement("div");
      tempDiv.innerHTML = text;
      const plainText = tempDiv.textContent || tempDiv.innerText;

      navigator.clipboard
        .writeText(plainText)
        .then(() => {
          ElMessage.success("复制成功");
        })
        .catch(() => {
          ElMessage.error("复制失败");
        });
    },
    copyToInput(text) {
      this.question = text;
      this.showModelPopup = false;
      this.showGradePopup = false;
    },
    handleModeChange(mode) {
      // 处理模式切换
      console.log('Mode changed to:', mode);
      // 可以在此处执行其他相关操作
    },
  },
};
</script>

<style scoped>
@import "@/assets/style/markdown.css";

/* 添加RAG模式相关样式 */
.mode-switch {
  margin-left: 20px;
}

.rag-container {
  height: calc(100vh - 60px);
  padding: 16px;
}
</style>