package excel.automessage.repository;

import excel.automessage.dto.message.log.MessageLogDetail;
import excel.automessage.entity.MessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {

    //상세 조회
    @Query("SELECT mh.storeName as storeName, ph.productName as productName " +
            "FROM MessageStorage ms " +
            "JOIN FETCH MessageHistory mh ON ms.messageStorageId = mh.messageStorage.messageStorageId " +
            "JOIN FETCH ProductHistory ph ON mh.historyId = ph.messageHistory.historyId " +
            "WHERE ms.messageStorageId = :messageStorageId")
    List<MessageLogDetail> findDetailLog(@Param("messageStorageId") String messageStorageId);
}
