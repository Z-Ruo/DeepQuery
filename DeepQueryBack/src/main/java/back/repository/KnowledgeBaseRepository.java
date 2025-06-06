package back.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import back.model.KnowledgeBase;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    
    Optional<KnowledgeBase> findByName(String name);
    
    boolean existsByCollectionName(String collectionName);
}
