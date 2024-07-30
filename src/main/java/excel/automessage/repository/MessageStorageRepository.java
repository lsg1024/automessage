package excel.automessage.repository;

import excel.automessage.entity.MessageStorage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageStorageRepository extends JpaRepository<MessageStorage, Long> {
}
