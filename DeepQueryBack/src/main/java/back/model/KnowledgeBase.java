package back.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "knowledge_bases")
@Data
public class KnowledgeBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String description;
    
    private String collectionName;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private int vectorDimension = 384; // default for AllMiniLmL6V2
    
    @OneToMany(mappedBy = "knowledgeBase")
    private List<DocumentInfo> documents;
}
