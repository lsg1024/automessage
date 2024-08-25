package excel.automessage.service.member;

import excel.automessage.BaseTest;
import excel.automessage.dto.members.MembersDTO;
import excel.automessage.entity.Members;
import excel.automessage.entity.Role;
import excel.automessage.repository.MembersRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
class MembersServiceTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MembersService membersService;

    @Autowired
    private MembersRepository membersRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    @DisplayName("테스트용 아이디 생성")
    void ApprovedID() {
        Members members = Members.builder()
                .memberId("UserId")
                .memberPassword(encoder.encode("UserPw"))
                .role(Role.USER)
                .build();

        membersRepository.save(members);

        log.info("테스트용 아이디 생성 완료");
    }

    @Test
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
    @DisplayName("중복 아이디 생성")
    void duplicateMember() {

        //given
        MembersDTO membersDTO = new MembersDTO("UserId", "UserPw");

        //when
        Boolean result = membersService.createMember(membersDTO);

        //then
        assertEquals(result, false);
    }

    @Test
    @DisplayName("아이디 생성")
    void createId() throws Exception {

        mockMvc.perform(post("/signup")
                        .param("memberId", "createId")
                        .param("memberPassword", "createPw")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

    }

    @Test
    @DisplayName("아이디 생성 실패")
    void createFailId() throws Exception {

        mockMvc.perform(post("/signup")
                        .param("memberId", "")
                        .param("memberPassword", "")
                        .with(csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(redirectedUrl(null));

    }

    @Test
    @DisplayName("로그인 성공")
    void loginSuccess() throws Exception {

        mockMvc.perform(post("/login")
                .param("memberId", "UserId")
                .param("memberPassword", "UserPw")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginSuccess"));

    }

    @Test
    @DisplayName("로그인 실패")
    void loginFail() throws Exception {

        mockMvc.perform(post("/login")
                        .param("memberId", "FailId")
                        .param("memberPassword", "FailPw")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

    }
}