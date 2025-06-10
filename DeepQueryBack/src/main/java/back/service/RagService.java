package back.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import back.entity.DocumentInfo;
import back.entity.DocumentVectorIds;
import back.model.ChatRequest;
import back.model.Message;
import back.model.RagQuestionRequest;
import back.model.RagQuestionResponse;
import back.model.RagSourceInfo;
import back.model.SystemResopnse;
import back.repository.DocumentRepository;
import back.repository.DocumentVectorIdsRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.response.ListCollectionsResp;

@Service
public class RagService {

    private final DocumentRepository documentRepository;
    private final DocumentVectorIdsRepository documentVectorIdsRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final MilvusClientV2 milvusClient;
    private final ChatService chatService;
    @Value("${milvus.host}")
    private String milvusHost;
    
    @Value("${milvus.port}")
    private int milvusPort;
    
    @Value("${milvus.username}")
    private String milvusUsername;
    
    @Value("${milvus.password}")
    private String milvusPassword;
    

    @Autowired
    public RagService(DocumentRepository documentRepository, 
                     DocumentVectorIdsRepository documentVectorIdsRepository,
                     EmbeddingModel embeddingModel,
                     EmbeddingStore<TextSegment> embeddingStore,
                     MilvusClientV2 milvusClient,
                     ChatService chatService) {
        this.documentRepository = documentRepository;
        this.documentVectorIdsRepository = documentVectorIdsRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.milvusClient = milvusClient;
        this.chatService = chatService;
    }

    // 上传文档并创建嵌入
    public DocumentInfo uploadDocument(MultipartFile file, String collectionName) throws Exception {

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名称不能为空");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }
        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("集合名称不能为空");
        }
        // 检查文件大小
        if (file.getSize() > 10 * 1024 * 1024) { // 限制为10MB
            throw new IllegalArgumentException("上传的文件大小不能超过10MB");
        }
        
        // 提取文件扩展名
        String fileType = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        // 创建临时文件
        Path tempFile = Files.createTempFile("upload_", "_" + fileName);

        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        // 根据文件类型选择合适的解析器
        DocumentParser parser;
        switch (fileType.toLowerCase()) {
            case "pdf" -> parser = new ApachePdfBoxDocumentParser();
            case "txt" -> parser = new TextDocumentParser();
            case "doc", "docx" -> parser = new ApachePoiDocumentParser();
            default -> {
                Files.deleteIfExists(tempFile); // Clean up temp file on error
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
            }
        }

        Document document = FileSystemDocumentLoader.loadDocument(tempFile, parser);

        // 将文档分段并创建嵌入
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);
        List<Embedding> embeddings = segments.stream()
                .map(segment -> embeddingModel.embed(segment).content())
                .toList();
        EmbeddingStore<TextSegment> collectionEmbeddingStore = MilvusEmbeddingStore.builder()
                .uri("http://" + milvusHost + ":" + milvusPort)
                .username(milvusUsername)
                .password(milvusPassword)
                .collectionName(collectionName)  // 使用传入的参数指定集合
                .build();

        // 将嵌入添加到指定集合中
        List<String> vectorIds = collectionEmbeddingStore.addAll(embeddings, segments);
        
        // 保存文档信息到数据库
        DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setFileName(file.getOriginalFilename());
        documentInfo.setFileType(fileType);
        documentInfo.setContent(document.text());
        documentInfo.setCollectionName(collectionName);
        
        // 先保存文档信息，获取文档ID
        documentInfo = documentRepository.save(documentInfo);
        
        // 将向量ID与文档关联
        for (int i = 0; i < vectorIds.size(); i++) {
            // 创建DocumentVectorIds对象并设置向量ID和文档信息
            DocumentVectorIds vectorId = new DocumentVectorIds();
            vectorId.setVectorId(vectorIds.get(i));
            vectorId.setDocument(documentInfo);
            // 保存到数据库
            documentVectorIdsRepository.save(vectorId);
        }
        
        // 清理临时文件
        Files.deleteIfExists(tempFile);

        return documentInfo;
    }

    public List<TextSegment> searchSimilarSegments(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        return embeddingStore.findRelevant(queryEmbedding, maxResults, 0.0)
                .stream()
                .map(match -> match.embedded())
                .toList();
    }
    
    public List<DocumentInfo> getAllDocuments() {
        return documentRepository.findAll();
    }
    // 删除指定名称的知识库
    public SystemResopnse deleteCollection(String name) {
        // 首先删除数据库中相关的文档记录
        try {
            List<DocumentInfo> documents = documentRepository.findByCollectionName(name);
            if (!documents.isEmpty()) {
                // 删除相关的向量ID记录
                for (DocumentInfo doc : documents) {
                    documentVectorIdsRepository.deleteByDocument(doc);
                }
                // 删除文档记录
                documentRepository.deleteAll(documents);
            }
        } catch (Exception e) {
            System.err.println("删除数据库记录时出错: " + e.getMessage());
        }
        
        // 删除Milvus集合
        DropCollectionReq dropCollectionReq = DropCollectionReq.builder()
        .collectionName(name)
        .build();
        milvusClient.dropCollection(dropCollectionReq);

        SystemResopnse request = new SystemResopnse();
        request.setStatus("success");
        request.setMessage("知识库 " + name + " 已删除");
        return request;
    }
    /**
     * 删除指定知识库中的特定文档
     * 
     * @param collectionName 知识库名称
     * @param documentId 文档ID
     * @return 删除结果
     */
    public SystemResopnse deleteDocument(String collectionName, Long documentId) {
        SystemResopnse response = new SystemResopnse();
        
        try {
            // 1. 查找文档
            DocumentInfo document = documentRepository.findById(documentId).orElse(null);
            if (document == null) {
                response.setStatus("error");
                response.setMessage("文档不存在");
                return response;
            }
            
            // 2. 验证文档属于指定集合
            if (!document.getCollectionName().equals(collectionName)) {
                response.setStatus("error");
                response.setMessage("文档不属于指定的知识库");
                return response;
            }
            
            // 3. 从Milvus中删除向量数据
            List<DocumentVectorIds> vectorIds = documentVectorIdsRepository.findByDocument(document);
            if (!vectorIds.isEmpty()) {
                EmbeddingStore<TextSegment> collectionEmbeddingStore = MilvusEmbeddingStore.builder()
                        .uri("http://" + milvusHost + ":" + milvusPort)
                        .username(milvusUsername)
                        .password(milvusPassword)
                        .collectionName(collectionName)
                        .build();
                
                // 删除向量 - 注意：LangChain4j的EmbeddingStore可能没有removeAll方法
                // 这里我们跳过向量删除，只删除数据库记录
                // 在实际应用中，可能需要直接使用Milvus客户端来删除向量
            }
            
            // 4. 删除数据库记录
            documentVectorIdsRepository.deleteByDocument(document);
            documentRepository.delete(document);
            
            response.setStatus("success");
            response.setMessage("文档删除成功");
            
        } catch (Exception e) {
            response.setStatus("error");
            response.setMessage("删除文档失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 根据文件名删除指定知识库中的文档
     * 
     * @param collectionName 知识库名称
     * @param fileName 文件名
     * @return 删除结果
     */
    public SystemResopnse deleteDocumentByFileName(String collectionName, String fileName) {
        SystemResopnse response = new SystemResopnse();
        
        try {
            // 查找指定集合中的文档
            List<DocumentInfo> documents = documentRepository.findByCollectionNameAndFileName(collectionName, fileName);
            
            if (documents.isEmpty()) {
                response.setStatus("error");
                response.setMessage("未找到指定的文档");
                return response;
            }
            
            // 删除找到的文档（通常应该只有一个）
            for (DocumentInfo document : documents) {
                SystemResopnse deleteResult = deleteDocument(collectionName, document.getId());
                if (!"success".equals(deleteResult.getStatus())) {
                    return deleteResult; // 如果删除失败，返回错误信息
                }
            }
            
            response.setStatus("success");
            response.setMessage("文档删除成功");
            
        } catch (Exception e) {
            response.setStatus("error");
            response.setMessage("删除文档失败: " + e.getMessage());
        }
        
        return response;
    }
    // 获取所有知识库
    public SystemResopnse listCollections() {
        ListCollectionsResp listAliasResp = milvusClient.listCollections();
        List<String> dbNames = listAliasResp.getCollectionNames();
        
        SystemResopnse request = new SystemResopnse();
        request.setStatus("success");
        request.setMessage("已获取知识库列表");
        request.setKnowledgeList(dbNames);
        return request;
    }
    // 创建新的知识库
    public SystemResopnse createCollection(String name, int dimension) {
        // 使用langchain4j提供的MilvusEmbeddingStore来创建集合
        // 这会自动创建一个具有正确架构的Milvus集合
        MilvusEmbeddingStore.builder()
                .uri("http://" + milvusHost + ":" + milvusPort)
                .username(milvusUsername)
                .password(milvusPassword)
                .collectionName(name)
                .dimension(dimension)
                .build();

        SystemResopnse request = new SystemResopnse();
        request.setStatus("success");
        request.setMessage("知识库 " + name + " 已创建");
        return request;
    }
    /**
     * 查询指定集合中的所有文档
     * 
     * @param collectionName 集合名称
     * @return 包含集合中文档信息的SystemResopnse
     */
    public SystemResopnse getCollectionDocuments(String collectionName) {
        SystemResopnse response = new SystemResopnse();
        
        try {
            // 验证集合名称
            if (!isValidCollectionName(collectionName)) {
                response.setStatus("error");
                response.setMessage("无效的集合名称: " + collectionName + "。集合名称必须以字母或下划线开头");
                return response;
            }
            
            // 获取所有集合，检查指定集合是否存在
            ListCollectionsResp listCollectionsResp = milvusClient.listCollections();
            List<String> collections = listCollectionsResp.getCollectionNames();
            
            if (!collections.contains(collectionName)) {
                response.setStatus("error");
                response.setMessage("集合 " + collectionName + " 不存在");
                return response;
            }
            
            // 从数据库中获取与此集合相关的所有文档
            List<DocumentInfo> documents = documentRepository.findByCollectionName(collectionName);
            
            if (documents.isEmpty()) {
                response.setStatus("warning");
                response.setMessage("未在集合 " + collectionName + " 中找到任何文档");
                response.setKnowledgeList(List.of());
            } else {
                // 提取所有文档名称
                List<String> documentNames = documents.stream()
                    .map(DocumentInfo::getFileName)
                    .toList();
                
                response.setStatus("success");
                response.setMessage("成功获取集合 " + collectionName + " 的文档列表，共 " + documentNames.size() + " 个文档");
                response.setKnowledgeList(documentNames);
            }
            
        } catch (Exception e) {
            response.setStatus("error");
            response.setMessage("获取集合文档失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 验证集合名称是否有效
     * Milvus要求集合名称必须以字母或下划线开头
     * 
     * @param name 要验证的集合名称
     * @return 如果名称有效则返回true，否则返回false
     */
    private boolean isValidCollectionName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        char firstChar = name.charAt(0);
        return Character.isLetter(firstChar) || firstChar == '_';
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ""; // Or throw an exception, based on how you want to handle this
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 执行RAG问答流程
     * 
     * @param request 包含查询、知识库名称等信息的RAG问题请求
     * @return RAG问答响应，包含答案和引用的源信息
     * @throws IOException 如果访问模型API出错
     */
    public RagQuestionResponse answerQuestion(RagQuestionRequest request) throws IOException {
        // 1. 从指定的知识库中检索相关的文档段落
        List<EmbeddingMatch<TextSegment>> relevantMatches = searchSpecificCollection(
            request.getQuery(), 
            request.getCollectionName(), 
            request.getMaxResults()
        );
        
        // 构建基础响应
        RagQuestionResponse response = new RagQuestionResponse();
        response.setQuery(request.getQuery());
        response.setCollectionName(request.getCollectionName());
        response.setTimestamp(Instant.now());
        response.setSessionId(request.getSessionId());
        
        // 如果没有找到相关文档
        if (relevantMatches.isEmpty()) {
            response.setAnswer("抱歉，在知识库中没有找到相关信息来回答您的问题。");
            response.setSources(new ArrayList<>());
            return response;
        }
        
        // 2. 准备源信息列表，并尝试查找原始文档名称
        List<RagSourceInfo> sourceInfos = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : relevantMatches) {
            RagSourceInfo sourceInfo = new RagSourceInfo();
            
            // 获取文本段落
            TextSegment segment = match.embedded();
            sourceInfo.setSegment(segment.text());
            sourceInfo.setScore(match.score());
            
            // 尝试从Metadata中提取文档信息，如果有的话
            String title = "文档片段";
            if (segment.metadata() != null && segment.metadata().get("file_name") != null) {
                title = segment.metadata().get("file_name").toString();
            }
            sourceInfo.setTitle(title);
            
            sourceInfos.add(sourceInfo);
        }
        
        // 3. 构建提示
        String prompt = buildRagPrompt(request.getQuery(), relevantMatches);
        
        // 4. 发送到LLM获取回答
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setStatus("zhipu");
        Message question = new Message();
        question.setRole("user");
        question.setContent(prompt);
        chatRequest.setQuestion(question);
        chatRequest.setZhipu_apiKey("1fc52571d7c3739e6b3a15382f9b300c.AWZdmov1xiwcyPnl");
        chatRequest.setSessionId(1);
        String answer = chatService.completions(chatRequest).getAnswer().getContent();
        
        // 5. 完善并返回响应
        response.setAnswer(answer);
        response.setSources(sourceInfos);
        return response;
    }
    
    /**
     * 从特定集合中搜索相关的文本段落
     * 
     * @param query 查询文本
     * @param collectionName 集合名称
     * @param maxResults 最大返回结果数
     * @return 相关文本段落的匹配列表
     */
    private List<EmbeddingMatch<TextSegment>> searchSpecificCollection(String query, String collectionName, int maxResults) {
        // 创建针对特定集合的嵌入存储
        EmbeddingStore<TextSegment> collectionEmbeddingStore = MilvusEmbeddingStore.builder()
                .uri("http://" + milvusHost + ":" + milvusPort)
                .username(milvusUsername)
                .password(milvusPassword)
                .collectionName(collectionName)
                .build();
        
        // 生成查询嵌入
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // 从特定集合中搜索相关内容
        return collectionEmbeddingStore.findRelevant(queryEmbedding, maxResults);
    }
    
    /**
     * 构建RAG提示，结合查询和相关上下文
     * 
     * @param query 用户查询
     * @param relevantMatches 相关文本段落
     * @return 构建的提示字符串
     */
    private String buildRagPrompt(String query, List<EmbeddingMatch<TextSegment>> relevantMatches) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 添加系统指令
        promptBuilder.append("您是一个知识丰富的AI助手，专注于基于提供的文档内容来回答用户问题。请遵循以下指导原则：\n\n");
        promptBuilder.append("1. 仅使用提供的上下文信息来回答问题\n");
        promptBuilder.append("2. 如果上下文信息不足以完整回答问题，请明确说明\n");
        promptBuilder.append("3. 避免编造不在上下文中的信息\n");
        promptBuilder.append("4. 保持回答简洁、专业且有逻辑性\n");
        promptBuilder.append("5. 针对技术性或学术性问题，尽可能提供详细解释\n\n");
        
        // 添加上下文信息
        promptBuilder.append("参考文档内容：\n");
        
        // 按相关性排序并添加文本段落
        for (int i = 0; i < relevantMatches.size(); i++) {
            EmbeddingMatch<TextSegment> match = relevantMatches.get(i);
            promptBuilder.append("---文档片段 ").append(i + 1).append("(相关度: ")
                .append(String.format("%.2f", match.score())).append(")---\n");
            promptBuilder.append(match.embedded().text().trim()).append("\n\n");
        }
        
        // 添加用户问题
        promptBuilder.append("---\n");
        promptBuilder.append("用户问题: ").append(query).append("\n\n");
        promptBuilder.append("基于以上参考文档内容，请提供详细而准确的回答。如果参考文档中没有足够信息，请明确说明该问题无法从提供的文档中得到完整解答。");
        
        return promptBuilder.toString();
    }
}
