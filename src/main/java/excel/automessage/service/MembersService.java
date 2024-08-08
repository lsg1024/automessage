package excel.automessage.service;

import excel.automessage.dto.members.MembersDTO;
import excel.automessage.repository.MembersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MembersService {

    private final MembersRepository membersRepository;

    public String createUser(MembersDTO membersDTO) {
        return membersRepository.save(membersDTO.toEntity()).getMemberId();
    }

}
