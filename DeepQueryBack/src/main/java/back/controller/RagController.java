package back.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import back.model.DocumentInfo;
import back.service.RagService;
import dev.langchain4j.data.segment.TextSegment;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    @Autowired
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentInfo> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            DocumentInfo documentInfo = ragService.uploadDocument(file);
            return ResponseEntity.ok(documentInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<TextSegment>> searchDocument(
            @RequestParam("query") String query,
            @RequestParam(value = "maxResults", defaultValue = "5") int maxResults) {
        List<TextSegment> results = ragService.searchSimilarSegments(query, maxResults);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentInfo>> getAllDocuments() {
        List<DocumentInfo> documents = ragService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }
    
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        try {
            ragService.deleteDocument(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    // 知识库（集合）的查询功能，查询所有知识库
    @GetMapping("/collections")
    public ResponseEntity<List<String>> getCollections() {
        try {
            List<String> collections = ragService.listCollections();
            return ResponseEntity.ok(collections);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/collections")
    public ResponseEntity<String> createCollection(
            @RequestParam("name") String name, 
            @RequestParam(value = "dimension", defaultValue = "384") int dimension) {
        try {
            ragService.createCollection(name, dimension);
            return ResponseEntity.ok("集合创建成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("创建失败: " + e.getMessage());
        }
    }
}
