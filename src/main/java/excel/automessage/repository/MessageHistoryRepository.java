package excel.automessage.repository;

import excel.automessage.entity.MessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {
}
