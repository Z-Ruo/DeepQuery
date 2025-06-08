package back.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import back.entity.DocumentInfo;
import back.model.CollectionCreateRequest;
import back.model.RagQuestionRequest;
import back.model.RagQuestionResponse;
import back.model.SearchRequest;
import back.model.SystemResopnse;
import back.service.RagService;

@RestController
@RequestMapping("/v1/rag")
public class RagController {

    private final RagService ragService;

    @Autowired
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    // 上传文档接口
    @PostMapping("/upload")
    public ResponseEntity<SystemResopnse> uploadDocument(@RequestParam("file") MultipartFile file,
        @RequestParam("collectionName") String collectionName) {
        try {
            SystemResopnse systemResopnse = new SystemResopnse();
            DocumentInfo documentInfo = ragService.uploadDocument(file, collectionName);
            systemResopnse.setStatus("success");
            systemResopnse.setMessage("文件上传成功");
            return ResponseEntity.ok(systemResopnse);
        } catch (Exception e) {
            SystemResopnse errorResponse = new SystemResopnse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("文件上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/search")
    public ResponseEntity<Object> searchDocument(@RequestBody SearchRequest request) {
        try {
            SystemResopnse systemResopnse = new SystemResopnse();
            systemResopnse.setStatus("success");
            systemResopnse.setMessage("搜索成功");
            return ResponseEntity.ok(systemResopnse);
        } catch (Exception e) {
            SystemResopnse errorResponse = new SystemResopnse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("搜索失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/documents")
    public ResponseEntity<Object> getAllDocuments() {
        try {
            List<DocumentInfo> documents = ragService.getAllDocuments();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "status", "error",
                    "message", "获取文档列表失败: " + e.getMessage()
                )
            );
        }
    }
    
    // 删除指定的知识库（集合）
    @PostMapping("/collections/delete/{name}")
    public ResponseEntity<SystemResopnse> deleteCollection(@PathVariable String name) {
        try {
            SystemResopnse response = ragService.deleteCollection(name);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            SystemResopnse errorResponse = new SystemResopnse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("删除知识库失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // 知识库（集合）的查询功能，查询所有知识库
    @PostMapping("/collections/list")
    public ResponseEntity<SystemResopnse> getCollections() {
        try {
            SystemResopnse collections = ragService.listCollections();
            return ResponseEntity.ok(collections);
        } catch (Exception e) {
            SystemResopnse errorResponse = new SystemResopnse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("获取知识库列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    // 创建新的知识库（集合）
    @PostMapping("/collections")
    public ResponseEntity<SystemResopnse> createCollection(@RequestBody CollectionCreateRequest request) {
        try {
            ragService.createCollection(request.getName(), request.getDimension());
            SystemResopnse response = new SystemResopnse();
            response.setStatus("success");
            response.setMessage("集合创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            SystemResopnse errorResponse = new SystemResopnse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("创建知识库失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 获取指定集合中的所有文档
     */
    @PostMapping("/collections/{name}/documents")
    public ResponseEntity<SystemResopnse> getCollectionDocuments(@PathVariable String name) {
        try {
            SystemResopnse response = ragService.getCollectionDocuments(name);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            SystemResopnse errorResponse = new SystemResopnse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("获取集合文档失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * RAG问答端点：根据知识库中的文档回答问题
     * 
     * @param request 包含问题、知识库名称等的请求
     * @return 回答响应，包含答案和引用的源信息
     */
    @PostMapping("/question")
    public ResponseEntity<?> answerQuestion(@RequestBody RagQuestionRequest request) {
        try {
            RagQuestionResponse response = ragService.answerQuestion(request);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            SystemResopnse errorResponse = new SystemResopnse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("处理RAG问答失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
