package excel.automessage.repository;

import excel.automessage.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Store findByStoreName(String storeName);
}
