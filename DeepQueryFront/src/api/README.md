# DeepQuery 前端API文档

## 概述

DeepQuery前端API模块提供了与后端服务交互的完整接口，主要分为四个模块：

- `auth`: 处理认证相关操作，如登录、注册和令牌管理
- `session`: 处理会话管理，包括创建会话、获取会话列表和会话消息
- `chat`: 处理聊天功能，包括获取聊天回复、流式聊天等
- `rag`: 处理RAG（检索增强生成）功能，包括知识库管理、文档上传和基于知识库的问答

## 安装和引入

所有API都已经封装好，可以通过以下方式引入：

```javascript
// 引入所有API
import api from '../api';

// 使用特定模块
api.auth.login('username', 'password');

// 或者按需引入
import { auth, chat } from '../api';
auth.login('username', 'password');
```

## 认证API (auth)

### 登录

```javascript
const response = await auth.login('username', 'password');
```

### 注册

```javascript
const response = await auth.register('username', 'password', 'phone');
```

### 注销

```javascript
auth.logout();
```

### 检查登录状态

```javascript
const isLoggedIn = auth.isLoggedIn();
```

### 获取当前用户名

```javascript
const username = auth.getCurrentUsername();
```

### 获取授权头信息

```javascript
const headers = auth.getAuthHeader();
```

### 更新用户信息

```javascript
const response = await auth.updateUserInfo(userId, {
  name: '新名称',
  avatar: '头像URL',
  // 其他需要更新的字段
});
```

### 获取用户信息

```javascript
const userInfo = await auth.getUserInfo(); // 获取当前用户
const otherUserInfo = await auth.getUserInfo(userId); // 获取指定用户
```

### 修改密码

```javascript
const response = await auth.changePassword('旧密码', '新密码');
```

## 会话API (session)

### 创建新会话

```javascript
const response = await session.startSession({
  userId: userId,
  // 其他会话初始化数据
});
```

### 获取会话列表

```javascript
const sessionList = await session.listSessions(userId);
```

### 获取上一个会话

```javascript
const prevSession = await session.getPreviousSession(userId);
```

### 获取会话消息

```javascript
const messages = await session.getSessionMessages(sessionId);
```

### 更新会话信息

```javascript
const response = await session.updateSession(sessionId, {
  title: '新会话标题',
  // 其他需要更新的字段
});
```

### 删除会话

```javascript
const response = await session.deleteSession(sessionId);
```

### 获取新会话ID

```javascript
const newSession = await session.getNewSessionId();
```

### 保存会话消息

```javascript
const response = await session.saveSessionMessage(sessionId, {
  role: 'user',
  content: '消息内容'
});
```

## 聊天API (chat)

### 获取聊天完成结果

```javascript
const messages = [
  { role: 'user', content: '你好，请介绍下自己' }
];

const response = await chat.getChatCompletions('ollama', messages, {
  modelName: 'qwen2.5:0.5b',
  sessionId: sessionId
});
```

### 流式聊天

```javascript
const messages = [
  { role: 'user', content: '写一首关于人工智能的诗' }
];

const response = await chat.streamChatCompletions('ollama', messages);

// 处理流式响应
const reader = response.body
  .pipeThrough(new TextDecoderStream('utf-8'))
  .pipeThrough(chat.parseSSE())
  .getReader();

let result = '';
while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  result += value.content;
  // 实时更新UI
  updateUI(result);
}
```

### 直接询问模型

```javascript
const answer = await chat.askModel('什么是量子计算？');
```

### 取消聊天请求

```javascript
await chat.cancelChatStream(requestId);
```

### 获取可用模型列表

```javascript
const models = await chat.getAvailableModels();
```

### 提交反馈

```javascript
await chat.submitFeedback(messageId, true, '这个回答很有帮助');
```

### 获取相关问题建议

```javascript
const questions = await chat.getRelatedQuestions('如何训练机器学习模型？');
```

## RAG API (rag)

### 获取知识库列表

```javascript
const knowledgeBases = await rag.getKnowledgeBases();
```

### 创建知识库

```javascript
const response = await rag.createKnowledgeBase('新知识库', 768);
```

### 获取知识库文档

```javascript
const documents = await rag.getCollectionDocuments('知识库名称');
```

### 删除知识库

```javascript
const response = await rag.deleteKnowledgeBase('知识库名称');
```

### 上传文档

```javascript
const file = document.querySelector('#fileInput').files[0];
const response = await rag.uploadDocument(file, '知识库名称');
```

### 批量上传文档

```javascript
const files = document.querySelector('#filesInput').files;
const response = await rag.uploadMultipleDocuments(
  files, 
  '知识库名称',
  (progress) => console.log(`上传进度: ${progress}%`)
);
```

### RAG问答

```javascript
const answer = await rag.askRagQuestion('什么是向量数据库？', '知识库名称', 3);
```

### 知识库搜索

```javascript
const results = await rag.searchInKnowledgeBase({
  query: '向量数据库',
  collectionName: '知识库名称'
});
```

### 获取所有文档

```javascript
const allDocuments = await rag.getAllDocuments();
```

### 重命名知识库

```javascript
const response = await rag.renameKnowledgeBase('旧名称', '新名称');
```

### 删除文档

```javascript
const response = await rag.deleteDocument('知识库名称', 'documentId');
```

### 获取知识库统计信息

```javascript
const stats = await rag.getKnowledgeBaseStats('知识库名称');
```

## 错误处理

所有API都包含了基本的错误处理，会在控制台输出错误信息，同时将错误抛出，便于调用者进行处理：

```javascript
try {
  await auth.login('username', 'wrong_password');
} catch (error) {
  // 处理登录失败
  console.error('登录失败:', error.message);
  showErrorMessage(error.message);
}
```

## 示例

查看 `src/examples/apiExamples.js` 文件获取更多使用示例。
