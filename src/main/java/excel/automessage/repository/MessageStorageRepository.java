package excel.automessage.repository;

import excel.automessage.entity.MessageStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageStorageRepository extends JpaRepository<MessageStorage, Long> {

    //전체 조회
    @Query("SELECT m FROM MessageStorage m WHERE m.lastModifiedDate <= :end")
    Page<MessageStorage> findByLastModifiedDateAll(@Param("end") String end, Pageable pageable);

    //기간 조회
    @Query("SELECT m FROM MessageStorage m WHERE m.lastModifiedDate BETWEEN :start AND :end")
    Page<MessageStorage> findByLastModifiedDate(@Param("start") String start, @Param("end") String end, Pageable pageable);


}
