package back.repository;

import back.model.DocumentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentInfo, Long> {
}
