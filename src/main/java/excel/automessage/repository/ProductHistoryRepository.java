package excel.automessage.repository;

import excel.automessage.entity.ProductHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductHistoryRepository extends JpaRepository<ProductHistory, Long> {
}
