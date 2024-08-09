package excel.automessage.repository;

import excel.automessage.entity.Members;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembersRepository extends JpaRepository<Members, Long> {

    boolean existsByMemberId(String memberId);

    Members findByMemberId(String memberId);

}
