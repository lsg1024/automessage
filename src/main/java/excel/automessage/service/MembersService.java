package excel.automessage.service;

import excel.automessage.dto.members.MembersDTO;
import excel.automessage.entity.Members;
import excel.automessage.entity.Role;
import excel.automessage.repository.MembersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MembersService {

    private final MembersRepository membersRepository;
    private final BCryptPasswordEncoder encoder;

    public Boolean createMember(MembersDTO membersDTO) {

        log.info("createMember {}", membersRepository.existsByMemberId(membersDTO.getMemberId()));
        if (membersRepository.existsByMemberId(membersDTO.getMemberId())) {
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

}
