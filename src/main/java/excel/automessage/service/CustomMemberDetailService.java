package excel.automessage.service;

import excel.automessage.dto.members.CustomMemberDetails;
import excel.automessage.entity.Members;
import excel.automessage.repository.MembersRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomMemberDetailService implements UserDetailsService {

    private final MembersRepository membersRepository;

    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {
        Members members = membersRepository.findByMemberId(memberId);

        if (members == null) {
            throw  new UsernameNotFoundException("사용자를 찾을 수 없습니다:" + memberId);
        }

        return new CustomMemberDetails(members); //JSESSIONID 쿠키 저장

    }
}
