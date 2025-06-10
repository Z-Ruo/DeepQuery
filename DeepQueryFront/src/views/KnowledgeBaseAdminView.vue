<template>
  <div class="knowledge-base-admin-container">
    <div class="admin-header">
      <h1>📚 知识库管理</h1>
      <el-button type="primary" @click="showCreateDialog = true" icon="Plus">
        新建知识库
      </el-button>
    </div>

    <!-- 知识库列表 -->
    <div class="knowledge-base-list">
      <el-card 
        v-for="collection in collections" 
        :key="collection" 
        class="collection-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <span class="collection-name">{{ collection }}</span>
            <div class="card-actions">
              <el-button 
                size="small" 
                @click="viewCollection(collection)"
                icon="View"
              >
                查看文件
              </el-button>
              <el-button 
                size="small" 
                type="primary" 
                @click="showUploadDialog(collection)"
                icon="Upload"
              >
                上传文档
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                @click="deleteCollection(collection)"
                icon="Delete"
              >
                删除
              </el-button>
            </div>
          </div>
        </template>
        
        <div class="collection-info">
          <el-descriptions :column="2" size="small">
            <el-descriptions-item label="文档数量">
              <el-tag>{{ collectionStats[collection]?.documentCount || '加载中...' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              <span>{{ collectionStats[collection]?.createdAt || '未知' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <div v-if="collections.length === 0 && !loading" class="empty-state">
      <div class="empty-icon">📚</div>
      <h3>暂无知识库</h3>
      <p>创建您的第一个知识库开始使用吧</p>
      <el-button type="primary" @click="showCreateDialog = true">
        创建知识库
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-loading 
        element-loading-text="正在加载知识库..."
        element-loading-background="rgba(122, 122, 122, 0.8)"
        v-loading="loading"
      />
    </div>

    <!-- 创建知识库对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建新知识库"
      width="500px"
      :before-close="handleCreateDialogClose"
    >
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input 
            v-model="createForm.name" 
            placeholder="请输入知识库名称"
            clearable
          />
          <div class="form-tip">
            名称必须以字母或下划线开头，只能包含字母、数字和下划线
          </div>
        </el-form-item>
        <el-form-item label="向量维度" prop="dimension">
          <el-input-number 
            v-model="createForm.dimension" 
            :min="128" 
            :max="2048" 
            :step="64"
            placeholder="向量维度"
          />
          <div class="form-tip">
            向量维度需要与使用的嵌入模型匹配，通常为384或768
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreateCollection" :loading="creating">
            {{ creating ? '创建中...' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 文件上传对话框 -->
    <el-dialog
      v-model="showUploadDialog_"
      title="上传文档"
      width="600px"
      :before-close="handleUploadDialogClose"
    >
      <div class="upload-section">
        <el-upload
          ref="uploadRef"
          class="upload-demo"
          drag
          :multiple="true"
          :auto-upload="false"
          :file-list="fileList"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :before-upload="beforeFileUpload"
          accept=".pdf,.doc,.docx,.txt"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖拽到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 PDF、DOC、DOCX、TXT 格式，单个文件不超过 10MB
            </div>
          </template>
        </el-upload>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showUploadDialog_ = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleUploadFiles" 
            :loading="uploading"
            :disabled="fileList.length === 0"
          >
            {{ uploading ? '上传中...' : '开始上传' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查看文件对话框 -->
    <el-dialog
      v-model="showViewDialog"
      :title="`${currentCollection} - 文档列表`"
      width="800px"
    >
      <div class="files-section">
        <div v-if="collectionFiles.length === 0" class="empty-files">
          <div class="empty-icon">📄</div>
          <p>该知识库暂无文档</p>
        </div>
        
        <el-table v-else :data="collectionFiles" style="width: 100%">
          <el-table-column prop="fileName" label="文件名" min-width="200">
            <template #default="scope">
              <div class="file-info">
                <el-icon><document /></el-icon>
                <span>{{ scope.row.fileName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="fileType" label="类型" width="80">
            <template #default="scope">
              <el-tag :type="getFileTypeColor(scope.row.fileType)" size="small">
                {{ scope.row.fileType.toUpperCase() }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button 
                size="small" 
                type="danger" 
                @click="deleteDocument(scope.row)"
                icon="Delete"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showViewDialog = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import { 
  getKnowledgeBases, 
  createKnowledgeBase, 
  deleteKnowledgeBase,
  getCollectionDocuments,
  uploadDocument,
  deleteDocumentByFileName
} from '@/api/rag'

export default {
  name: 'KnowledgeBaseAdminView',
  components: {
    UploadFilled,
    Document
  },
  setup() {
    // 响应式数据
    const collections = ref([])
    const collectionStats = ref({})
    const loading = ref(false)
    const creating = ref(false)
    const uploading = ref(false)
    
    // 对话框状态
    const showCreateDialog = ref(false)
    const showUploadDialog_ = ref(false)
    const showViewDialog = ref(false)
    
    // 表单数据
    const createForm = reactive({
      name: '',
      dimension: 384
    })
    
    const createRules = {
      name: [
        { required: true, message: '请输入知识库名称', trigger: 'blur' },
        { 
          pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, 
          message: '名称必须以字母或下划线开头，只能包含字母、数字和下划线', 
          trigger: 'blur' 
        },
        { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
      ],
      dimension: [
        { required: true, message: '请输入向量维度', trigger: 'blur' },
        { type: 'number', min: 128, max: 2048, message: '维度范围在 128 到 2048 之间', trigger: 'blur' }
      ]
    }
    
    // 文件相关
    const fileList = ref([])
    const currentUploadCollection = ref('')
    const currentCollection = ref('')
    const collectionFiles = ref([])
    
    // refs
    const createFormRef = ref(null)
    const uploadRef = ref(null)

    // 加载知识库列表
    const loadCollections = async () => {
      loading.value = true
      try {
        const response = await getKnowledgeBases()
        if (response.status === 'success' && response.knowledgeList) {
          collections.value = response.knowledgeList
          // 加载每个知识库的统计信息
          await loadCollectionStats()
        } else {
          collections.value = []
        }
      } catch (error) {
        console.error('加载知识库列表失败:', error)
        ElMessage.error('加载知识库列表失败')
        collections.value = []
      } finally {
        loading.value = false
      }
    }

    // 加载知识库统计信息
    const loadCollectionStats = async () => {
      for (const collection of collections.value) {
        try {
          const response = await getCollectionDocuments(collection)
          if (response.status === 'success') {
            collectionStats.value[collection] = {
              documentCount: response.knowledgeList ? response.knowledgeList.length : 0,
              createdAt: '未知' // 可以从其他API获取
            }
          }
        } catch (error) {
          console.error(`获取知识库 ${collection} 统计信息失败:`, error)
          collectionStats.value[collection] = {
            documentCount: 0,
            createdAt: '未知'
          }
        }
      }
    }

    // 创建知识库
    const handleCreateCollection = async () => {
      if (!createFormRef.value) return
      
      try {
        await createFormRef.value.validate()
        creating.value = true
        
        const response = await createKnowledgeBase(createForm.name, createForm.dimension)
        if (response.status === 'success') {
          ElMessage.success('知识库创建成功')
          showCreateDialog.value = false
          createForm.name = ''
          createForm.dimension = 384
          await loadCollections()
        } else {
          ElMessage.error(response.message || '创建知识库失败')
        }
      } catch (error) {
        console.error('创建知识库失败:', error)
        ElMessage.error('创建知识库失败')
      } finally {
        creating.value = false
      }
    }

    // 删除知识库
    const deleteCollection = async (collectionName) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除知识库 "${collectionName}" 吗？此操作不可恢复。`,
          '警告',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        const response = await deleteKnowledgeBase(collectionName)
        if (response.status === 'success') {
          ElMessage.success('知识库删除成功')
          await loadCollections()
        } else {
          ElMessage.error(response.message || '删除知识库失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除知识库失败:', error)
          ElMessage.error('删除知识库失败')
        }
      }
    }

    // 显示上传对话框
    const showUploadDialog = (collectionName) => {
      currentUploadCollection.value = collectionName
      showUploadDialog_.value = true
      fileList.value = []
    }

    // 处理文件变化
    const handleFileChange = (file, fileList_) => {
      // 更新文件列表
      fileList.value = fileList_
    }

    // 文件上传前的验证
    const beforeFileUpload = (file) => {
      // 验证文件类型和大小
      const validTypes = ['pdf', 'doc', 'docx', 'txt']
      const fileType = file.name.split('.').pop().toLowerCase()
      
      if (!validTypes.includes(fileType)) {
        ElMessage.error('不支持的文件类型，请上传 PDF、DOC、DOCX 或 TXT 文件')
        return false
      }
      
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.error('文件大小不能超过 10MB')
        return false
      }
      
      return true
    }

    // 移除文件
    const handleFileRemove = (file, fileList_) => {
      fileList.value = fileList_
    }

    // 上传文件
    const handleUploadFiles = async () => {
      if (fileList.value.length === 0) {
        ElMessage.warning('请选择要上传的文件')
        return
      }
      
      uploading.value = true
      let successCount = 0
      let failCount = 0
      
      try {
        // 逐个上传文件
        for (const fileItem of fileList.value) {
          try {
            const response = await uploadDocument(fileItem.raw, currentUploadCollection.value)
            if (response.status === 'success') {
              successCount++
              console.log(`文件 "${fileItem.name}" 上传成功`)
            } else {
              failCount++
              console.error(`文件 "${fileItem.name}" 上传失败: ${response.message}`)
              ElMessage.error(`文件 "${fileItem.name}" 上传失败: ${response.message}`)
            }
          } catch (error) {
            failCount++
            console.error(`文件 "${fileItem.name}" 上传异常:`, error)
            ElMessage.error(`文件 "${fileItem.name}" 上传失败`)
          }
        }
        
        // 显示总体结果
        if (successCount > 0) {
          ElMessage.success(`成功上传 ${successCount} 个文件`)
        }
        if (failCount > 0) {
          ElMessage.warning(`${failCount} 个文件上传失败`)
        }
        
        // 关闭对话框并刷新数据
        if (successCount > 0) {
          showUploadDialog_.value = false
          fileList.value = []
          await loadCollections() // 刷新统计信息
        }
      } catch (error) {
        console.error('批量上传文件失败:', error)
        ElMessage.error('上传文件失败')
      } finally {
        uploading.value = false
      }
    }

    // 查看知识库文件
    const viewCollection = async (collectionName) => {
      currentCollection.value = collectionName
      showViewDialog.value = true
      
      try {
        const response = await getCollectionDocuments(collectionName)
        if (response.status === 'success' && response.knowledgeList) {
          // 这里需要调用获取详细文档信息的API
          // 目前先显示文件名列表
          collectionFiles.value = response.knowledgeList.map(fileName => ({
            fileName: fileName,
            fileType: fileName.split('.').pop().toLowerCase(),
            createdAt: new Date().toISOString(), // 临时数据
            id: fileName // 临时ID
          }))
        } else {
          collectionFiles.value = []
        }
      } catch (error) {
        console.error('获取文档列表失败:', error)
        ElMessage.error('获取文档列表失败')
        collectionFiles.value = []
      }
    }

    // 删除文档
    const deleteDocument = async (document) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除文档 "${document.fileName}" 吗？`,
          '警告',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        // 调用删除文档的API
        const response = await deleteDocumentByFileName(currentCollection.value, document.fileName)
        if (response.status === 'success') {
          ElMessage.success('文档删除成功')
          await viewCollection(currentCollection.value) // 刷新文件列表
        } else {
          ElMessage.error(response.message || '删除文档失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除文档失败:', error)
          ElMessage.error('删除文档失败')
        }
      }
    }

    // 工具函数
    const getFileTypeColor = (fileType) => {
      const colorMap = {
        'pdf': 'danger',
        'doc': 'primary',
        'docx': 'primary',
        'txt': 'info'
      }
      return colorMap[fileType.toLowerCase()] || 'info'
    }

    const formatDate = (dateString) => {
      if (!dateString) return '未知'
      return new Date(dateString).toLocaleString('zh-CN')
    }

    // 对话框关闭处理
    const handleCreateDialogClose = () => {
      createForm.name = ''
      createForm.dimension = 384
      if (createFormRef.value) {
        createFormRef.value.clearValidate()
      }
    }

    const handleUploadDialogClose = () => {
      fileList.value = []
      currentUploadCollection.value = ''
    }

    // 生命周期
    onMounted(() => {
      loadCollections()
    })

    return {
      // 数据
      collections,
      collectionStats,
      loading,
      creating,
      uploading,
      
      // 对话框状态
      showCreateDialog,
      showUploadDialog_,
      showViewDialog,
      
      // 表单
      createForm,
      createRules,
      createFormRef,
      
      // 文件相关
      fileList,
      currentUploadCollection,
      currentCollection,
      collectionFiles,
      uploadRef,
      
      // 方法
      loadCollections,
      handleCreateCollection,
      deleteCollection,
      showUploadDialog,
      handleFileChange,
      beforeFileUpload,
      handleFileRemove,
      handleUploadFiles,
      viewCollection,
      deleteDocument,
      getFileTypeColor,
      formatDate,
      handleCreateDialogClose,
      handleUploadDialogClose
    }
  }
}
</script>

<style scoped>
.knowledge-base-admin-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.admin-header h1 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.knowledge-base-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.collection-card {
  transition: transform 0.2s;
}

.collection-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.collection-name {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.collection-info {
  margin-top: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h3 {
  margin: 16px 0 8px 0;
  color: #606266;
}

.loading-state {
  height: 300px;
  position: relative;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.upload-section {
  margin-bottom: 20px;
}

.upload-demo {
  width: 100%;
}

.files-section {
  max-height: 400px;
  overflow-y: auto;
}

.empty-files {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.empty-files .empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-info span {
  word-break: break-all;
}

.dialog-footer {
  display: flex;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .knowledge-base-list {
    grid-template-columns: 1fr;
  }
  
  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .card-actions {
    flex-wrap: wrap;
  }
}
</style>
