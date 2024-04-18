package excel.automessage.repository;

import excel.automessage.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

interface StoreRepository extends JpaRepository<Store, Long> {
}
