package excel.automessage.repository;

import excel.automessage.dto.message.log.MessageStorageDTO;
import excel.automessage.entity.MessageStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageStorageRepository extends JpaRepository<MessageStorage, Long> {

    @Query("SELECT m FROM MessageStorage m WHERE m.lastModifiedDate <= :end")
    Page<MessageStorage> findByLastModifiedDateAll(@Param("end") String end, Pageable pageable);
    @Query("SELECT m FROM MessageStorage m WHERE m.lastModifiedDate BETWEEN :start AND :end")
    Page<MessageStorage> findByLastModifiedDate(@Param("start") String start, @Param("end") String end, Pageable pageable);

}
