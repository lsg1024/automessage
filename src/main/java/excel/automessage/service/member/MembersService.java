package excel.automessage.service.member;

import excel.automessage.dto.members.AdminMembersDTO;
import excel.automessage.dto.members.MembersDTO;
import excel.automessage.entity.Members;
import excel.automessage.entity.Role;
import excel.automessage.repository.MembersRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Timed("otalk.member")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembersService {

    private final MembersRepository membersRepository;
    private final BCryptPasswordEncoder encoder;

    @Transactional
    public Boolean createMember(MembersDTO membersDTO) {

        boolean result = membersRepository.existsByMemberId(membersDTO.getMemberId());
        log.info("createMember {}", result);
        if (result) {
            return false;
        }

        Members members = Members.builder()
                .memberId(membersDTO.getMemberId())
                .memberPassword(encoder.encode(membersDTO.getMemberPassword()))
                .role(Role.WAIT)
                .build();

        membersRepository.save(members);

        return true;
    }

    public Page<Members> membersPage(int page, int size, String memberId) {
        Pageable pageable = PageRequest.of(page, size);
        return membersRepository.findByMemberIdNot(memberId, pageable);
    }

    public AdminMembersDTO.Members findById(Long id) {
        Members member = membersRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 아이디 입니다."));

        return new AdminMembersDTO.Members(member.getUserId(), member.getMemberId(), member.getRole().toString());
    }

    public void adminUpdateRole(Long id, AdminMembersDTO.Members memberDTO) {
        Members member = membersRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 아이디 입니다."));

        member.updateRole(memberDTO.getMemberId(), memberDTO.getMemberRole());
    }

    @Transactional
    public void adminDelete(Long id) {
        membersRepository.deleteById(id);
    }
}
