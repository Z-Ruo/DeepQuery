package back.repository;


import back.entity.HistoryRecordInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistoryRecordsRepository extends JpaRepository<HistoryRecordInfo, Long> {
    List<HistoryRecordInfo> findById(Integer userId);

    @Query("SELECT h FROM HistoryRecordInfo h WHERE h.session.id = :sessionId ORDER BY h.timestamp DESC")
    List<HistoryRecordInfo> findRecentBySessionId(@Param("sessionId") Integer sessionId, Pageable pageable);
}
