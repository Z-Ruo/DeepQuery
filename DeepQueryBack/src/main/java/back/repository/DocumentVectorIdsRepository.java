package back.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import back.entity.DocumentInfo;
import back.entity.DocumentVectorIds;



@Repository
public interface DocumentVectorIdsRepository extends JpaRepository<DocumentVectorIds, Long> {
    // 根据文档查找所有相关的向量ID
    List<DocumentVectorIds> findByDocument(DocumentInfo document);
    
    // 删除指定文档的所有向量ID记录
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentVectorIds d WHERE d.document = :document")
    void deleteByDocument(@Param("document") DocumentInfo document);
}
