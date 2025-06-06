package back.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import back.model.DocumentInfo;
import back.repository.DocumentRepository;
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
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.response.ListCollectionsResp;

@Service
public class RagService {

    private final DocumentRepository documentRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final MilvusClientV2 milvusClient;
    @Value("${milvus.host}")
    private String milvusHost;
    
    @Value("${milvus.port}")
    private int milvusPort;
    
    @Value("${milvus.username}")
    private String milvusUsername;
    
    @Value("${milvus.password}")
    private String milvusPassword;
    
    @Value("${milvus.dimension}")
    private int dimension;

    @Autowired
    public RagService(DocumentRepository documentRepository, EmbeddingModel embeddingModel,
                     EmbeddingStore<TextSegment> embeddingStore,MilvusClientV2 milvusClient) {
        this.documentRepository = documentRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.milvusClient = milvusClient;
    }

    public DocumentInfo uploadDocument(MultipartFile file) throws Exception {
        // 创建临时文件
        Path tempFile = Files.createTempFile("upload_", "_" + file.getOriginalFilename());
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        // 根据文件类型选择合适的解析器
        DocumentParser parser;
        String fileType = getFileExtension(file.getOriginalFilename());

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

        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }

        // 保存文档信息到数据库
        DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setFileName(file.getOriginalFilename());
        documentInfo.setFileType(fileType);
        documentInfo.setContent(document.text());

        // 清理临时文件
        Files.deleteIfExists(tempFile);

        return documentRepository.save(documentInfo);
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
    
    public void deleteDocument(Long id) {
        Optional<DocumentInfo> documentOpt = documentRepository.findById(id);
        if (documentOpt.isPresent()) {
            // 从数据库中删除文档记录
            documentRepository.deleteById(id);
            
            // 注意：当前实现不会从Milvus中删除对应的向量
            // 因为我们没有存储文档ID与向量ID之间的映射关系
            // 要实现完整的删除功能，需要额外设计这种映射关系
        } else {
            throw new IllegalArgumentException("文档不存在: " + id);
        }
    }
    
    public List<String> listCollections() {
        ListCollectionsResp listAliasResp = milvusClient.listCollections();
        List<String> dbNames = listAliasResp.getCollectionNames();
        return dbNames;
    }
    
    public void createCollection(String name, int dimension) {
        // 使用langchain4j提供的MilvusEmbeddingStore来创建集合
        // 这会自动创建一个具有正确架构的Milvus集合
        MilvusEmbeddingStore.builder()
                .uri("http://" + milvusHost + ":" + milvusPort)
                .username(milvusUsername)
                .password(milvusPassword)
                .collectionName(name)
                .dimension(dimension)
                .build();
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ""; // Or throw an exception, based on how you want to handle this
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
