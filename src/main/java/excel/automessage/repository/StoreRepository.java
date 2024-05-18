package excel.automessage.repository;

import excel.automessage.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByStoreName(String storeName);

    // 전체 메뉴 검색
    List<Store> findByStoreNameContains(String storeName);

    // null 검색
    List<Store> findByStoreNameIsNotNullAndStoreNameContains(String storeName);

    // notnull 검색
    List<Store> findByStoreNameIsNullAndStoreNameContains(String storeName);
}
