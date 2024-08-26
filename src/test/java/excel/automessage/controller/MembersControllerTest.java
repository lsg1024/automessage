package excel.automessage.controller;

import excel.automessage.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
class MembersControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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