package excel.automessage.repository;

import excel.automessage.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByStoreName(String storeName);

    // 조건과 검색어에 따른 동적 쿼리
    @Query("SELECT s FROM Store s WHERE " +
            "(:category = 'all' OR " +
            "(:category = 'null' AND s.storePhoneNumber IS NULL) OR " +
            "(:category = 'notnull' AND s.storePhoneNumber IS NOT NULL)) " +
            "AND (:storeName IS NULL OR s.storeName LIKE %:storeName%)")
    Page<Store> findByCategoryAndStoreName(@Param("category") String category, @Param("storeName") String storeName, Pageable pageable);
}
