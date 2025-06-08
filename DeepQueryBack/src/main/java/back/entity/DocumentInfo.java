package back.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "documents")
public class DocumentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "collection_name", nullable = false)
    private String collectionName;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
