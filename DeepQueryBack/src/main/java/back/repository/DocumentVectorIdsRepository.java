package back.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import back.entity.DocumentVectorIds;

@Repository
public interface DocumentVectorIdsRepository extends JpaRepository<DocumentVectorIds, Long> {
    // 可以添加自定义查询方法，如按文档ID查找所有向量ID等
}
