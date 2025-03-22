package excel.automessage.repository;

import excel.automessage.entity.Members;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembersRepository extends JpaRepository<Members, Long> {

    boolean existsByMemberId(String memberId);

    Members findByMemberId(String memberId);

    // 나빼고
    Page<Members> findByMemberIdNot(String memberId, Pageable pageable);

}
