package excel.automessage.controller;

import excel.automessage.BaseTest;
import excel.automessage.dto.message.MessageDTO;
import excel.automessage.entity.*;
import excel.automessage.repository.MembersRepository;
import excel.automessage.repository.MessageStorageRepository;
import excel.automessage.service.redis.IdempotencyRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.FlashMap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
class MessageControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdempotencyRedisService idempotencyRedisService;

    private static MockHttpSession session;

    @BeforeAll
    @DisplayName("API 테스트 로그인")
    static void setUp(@Autowired MembersRepository membersRepository,
                      @Autowired BCryptPasswordEncoder encoder,
                      @Autowired MessageStorageRepository messageStorageRepository,
                      @Autowired MockMvc mockMvc) throws Exception {

        membersRepository.deleteAll();
        messageStorageRepository.deleteAll();

        Members members = Members.builder()
                .memberId("TestId")
                .memberPassword(encoder.encode("TestPw"))
                .role(Role.USER)
                .build();

        membersRepository.save(members);

        MvcResult mvcResult = mockMvc.perform(post("/login")
                        .param("memberId", "TestId")
                        .param("memberPassword", "TestPw")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginSuccess"))
                .andReturn();

        session = (MockHttpSession) mvcResult.getRequest().getSession();

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setTo("testReceiver");
        messageDTO.setContent("testContent");
        messageDTO.setStoreName("testStore");
        messageDTO.setProductName(List.of("testProduct1", "testProduct2"));

        List<ProductHistory> productHistoryList = messageDTO.getProductName().stream().map(productName ->
                ProductHistory.builder()
                        .productName("testProduct1")
                        .build()
        ).toList();

        List<MessageHistory> messageHistories = new ArrayList<>();

        messageHistories.add(MessageHistory.builder()
                .receiver(messageDTO.getTo())
                .content(messageDTO.getContent())
                .status("200")
                .errorMessage("")
                .storeName(messageDTO.getStoreName())
                .productNames(productHistoryList).build());

        MessageStorage messageStorage = MessageStorage.builder()
                .messageHistories(new ArrayList<>())
                .build();

        for (MessageHistory messageHistory : messageHistories) {
            log.info("before All messageHistory data = {}", messageHistory.getStoreName());
            messageStorage.addMessageHistory(messageHistory);
        }

        messageStorageRepository.save(messageStorage);

    }

    // 메시지 양식 업로드 (엑셀)
    @Test
    @DisplayName("메시지 양식 업로드 성공")
    void messageUploadSuccess() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "test file".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/automessage/message/file_send")
                        .file(mockMultipartFile)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/message/content"));

    }

    // 메시지 양식 업로드 (타입 오류)
    @Test
    @DisplayName("메시지 양식 타입 오류 실패")
    @Transactional
    void messageUploadFail_Type() throws Exception {

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "test.html", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "test file".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/automessage/message/file_send")
                        .file(mockMultipartFile)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/message"))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("errorMessage")).isEqualTo("엑셀 파일만 업로드 가능합니다.");

    }

    // 메시지 양식 업로드 (파일 null)
    @Test
    @DisplayName("메시지 양식 null 오류 실패")
    @Transactional
    void messageUploadFail_Null() throws Exception {

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        MvcResult result = mockMvc.perform(multipart("/automessage/message/file_send")
                        .file(mockMultipartFile)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/message"))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("errorMessage")).isEqualTo("파일을 선택해주세요.");

    }

    // 메시지 중복 전송
//    @Test
//    @DisplayName("메시지 중복 전송")
//    @Transactional
//    void messageDuplicateSend() throws Exception {
//        String idempotencyKey = UUID.randomUUID().toString();
//
//        MessageFormEntry messageFormEntry = new MessageFormEntry();
//        messageFormEntry.getPhone().put("testKey", "testPhoneNumber");
//        messageFormEntry.getSmsForm().put("testSmsKey", List.of("testContent1", "testContent2"));
//        messageFormEntry.setMissingStores(List.of("testStore1", "testStore2"));
//        messageFormEntry.setContent("testContent");
//        messageFormEntry.setSendSms(true);
//
//        MessageListDTO messageListDTO = new MessageListDTO();
//        messageListDTO.getMessageListDTO().add(messageFormEntry);
//
//        // 첫 번째 요청
//        mockMvc.perform(post("/automessage/message/content")
//                        .param("idempotencyKey", idempotencyKey)
//                        .session(session)
//                        .flashAttr("messageForm", messageListDTO)
//                        .with(csrf()))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/automessage/message/result"));
//
//        // 중복 요청 확인
//        assertThat(idempotencyRedisService.isDuplicateRequest(idempotencyKey)).isTrue();
//
//        // 두 번째 요청 - 중복 요청
//        mockMvc.perform(post("/automessage/message/content")
//                        .param("idempotencyKey", idempotencyKey)
//                        .session(session)
//                        .flashAttr("messageForm", messageListDTO)
//                        .with(csrf()))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/automessage/message/result"))
//                .andExpect(result -> {
//                    FlashMap flashMap = result.getFlashMap();
//                    assertThat(flashMap.get("responses")).isEqualTo("중복데이터 발생");
//                });
//
//        // 중복 요청이 제대로 처리되었는지 확인
//        assertThat(idempotencyRedisService.isDuplicateRequest(idempotencyKey)).isTrue();
//    }


    // 메시지 전송
//    @Test
//    @DisplayName("메시지 전송 성공")
//    void messageSendSuccess() throws Exception {
//
//        mockMvc.perform(post("/automessage/message/content")
//                .session(session)
//                .with(csrf()));
//    }

    // 메시지 로그 조회 (get)
    @Test
    @DisplayName("메시지 로그 조회 성공")
    @Transactional
    void messageLogSuccess() throws Exception {

        mockMvc.perform(get("/automessage/message/log?")
                .session(session)
                .with(csrf())
                .param("page", "1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(redirectedUrl(null));

    }

    @Test
    @DisplayName("메시지 로그 조회 실패 page = null")
    @Transactional
    void messageLogFail() throws Exception {

        MvcResult result = mockMvc.perform(get("/automessage/message/log?")
                        .session(session)
                        .with(csrf())
                        .param("page", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/message/log?"))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("errorMessage")).isEqualTo("잘못된 경로 입니다.");

    }

    // 메시지 상세 페이지 (get)
    @Test
    @DisplayName("메시지 상세 페이지 조회 성공")
    @Transactional
    void messageLogDetailPageSuccess() throws Exception {

        String messageId = "1";

        mockMvc.perform(get("/automessage/message/log/{id}", messageId)
                        .session(session)
                        .with(csrf())
                        .param("id", messageId))
                .andExpect(status().is2xxSuccessful())
                .andExpect(redirectedUrl(null));

    }

    @Test
    @DisplayName("메시지 상세 페이지 조회 실패 (id 없음)")
    @Transactional
    void messageLogDetailPageFail() throws Exception {

        String messageId = "2";

        mockMvc.perform(get("/automessage/message/log/{id}", messageId)
                        .session(session)
                        .with(csrf())
                        .param("id", messageId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/message/log?"));

    }

}