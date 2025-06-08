package back.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import back.entity.DocumentInfo;


public interface DocumentRepository extends JpaRepository<DocumentInfo, Long> {
    
    // 根据知识库的集合名称查询文档
    List<DocumentInfo> findByCollectionName(String collectionName);
    
    // 根据ID查询文档信息
    @Query("SELECT d FROM DocumentInfo d WHERE d.id = :id")
    DocumentInfo findDocumentById(@Param("id") Long id);
}
