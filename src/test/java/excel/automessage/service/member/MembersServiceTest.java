package excel.automessage.service.member;

import excel.automessage.BaseTest;
import excel.automessage.dto.members.MembersDTO;
import excel.automessage.entity.Members;
import excel.automessage.entity.Role;
import excel.automessage.repository.MembersRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@AutoConfigureMockMvc
class MembersServiceTest extends BaseTest {

    @Autowired
    private MembersService membersService;

    @BeforeAll
    @DisplayName("테스트용 아이디 생성")
    static void ApprovedID(@Autowired MembersRepository membersRepository,
                           @Autowired BCryptPasswordEncoder encoder) {
        Members members = Members.builder()
                .memberId("UserId")
                .memberPassword(encoder.encode("UserPw"))
                .role(Role.USER)
                .build();

        membersRepository.save(members);

        log.info("테스트용 아이디 생성 완료");
    }

    @Test
    @Transactional
    @DisplayName("아이디 생성")
    void createMember() {

        //given
        MembersDTO membersDTO = new MembersDTO("createId", "createPw");

        //when
        Boolean result = membersService.createMember(membersDTO);

        //then
        assertEquals(result, true);


    }

    @Test
    @Transactional
    @DisplayName("중복 아이디 생성")
    void duplicateMember() {

        //given
        MembersDTO membersDTO = new MembersDTO("UserId", "UserPw");

        //when
        Boolean result = membersService.createMember(membersDTO);

        //then
        assertEquals(result, false);
    }

}