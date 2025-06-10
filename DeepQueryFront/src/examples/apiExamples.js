// API使用示例

import { auth, session, chat, rag } from '../api';

// 认证示例
async function authExample() {
  try {
    // 登录
    const loginResult = await auth.login('username', 'password');
    console.log('登录结果:', loginResult);
    
    // 检查登录状态
    const isLoggedIn = auth.isLoggedIn();
    console.log('是否已登录:', isLoggedIn);
    
    // 获取用户信息
    const userInfo = await auth.getUserInfo();
    console.log('用户信息:', userInfo);
    
  } catch (error) {
    console.error('认证示例错误:', error);
  }
}

// 会话管理示例
async function sessionExample() {
  try {
    // 创建新会话
    const newSession = await session.getNewSessionId();
    console.log('新会话:', newSession);
    
    // 获取会话列表
    const userId = localStorage.getItem('userId'); // 假设有保存用户ID
    const sessionList = await session.listSessions(userId);
    console.log('会话列表:', sessionList);
    
    // 获取会话消息
    if (sessionList && sessionList.length > 0) {
      const sessionId = sessionList[0].sessionId;
      const messages = await session.getSessionMessages(sessionId);
      console.log('会话消息:', messages);
    }
  } catch (error) {
    console.error('会话示例错误:', error);
  }
}

// 聊天示例
async function chatExample() {
  try {
    // 简单请求
    const question = "什么是人工智能？";
    const messages = [{ role: 'user', content: question }];
    const response = await chat.getChatCompletions('ollama', messages);
    console.log('AI回答:', response);
    
    // 流式请求
    const streamResponse = await chat.streamChatCompletions('ollama', messages);
    const reader = streamResponse.body
      .pipeThrough(new TextDecoderStream("utf-8"))
      .pipeThrough(chat.parseSSE())
      .getReader();
    
    console.log('流式回答:');
    // 读取流式数据
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      console.log(value.content);
    }
    
    // 获取可用模型
    const models = await chat.getAvailableModels();
    console.log('可用模型:', models);
    
  } catch (error) {
    console.error('聊天示例错误:', error);
  }
}

// RAG示例
async function ragExample() {
  try {
    // 获取知识库列表
    const knowledgeBases = await rag.getKnowledgeBases();
    console.log('知识库列表:', knowledgeBases);
    
    if (knowledgeBases && knowledgeBases.knowledgeList && knowledgeBases.knowledgeList.length > 0) {
      const kbName = knowledgeBases.knowledgeList[0];
      
      // 获取知识库文档
      const documents = await rag.getCollectionDocuments(kbName);
      console.log('知识库文档:', documents);
      
      // 使用RAG回答问题
      const ragAnswer = await rag.askRagQuestion('什么是向量数据库？', kbName, 3);
      console.log('RAG回答:', ragAnswer);
    } else {
      // 创建新的知识库
      const createResult = await rag.createKnowledgeBase('test_knowledge_base');
      console.log('创建知识库结果:', createResult);
    }
    
  } catch (error) {
    console.error('RAG示例错误:', error);
  }
}

// 执行示例
export async function runExamples() {
  console.log('=== 认证示例 ===');
  await authExample();
  
  console.log('\n=== 会话示例 ===');
  await sessionExample();
  
  console.log('\n=== 聊天示例 ===');
  await chatExample();
  
  console.log('\n=== RAG示例 ===');
  await ragExample();
}

// 导出各个示例函数
export {
  authExample,
  sessionExample,
  chatExample,
  ragExample
};
