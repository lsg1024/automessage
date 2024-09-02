package excel.automessage.controller;

import excel.automessage.BaseTest;
import excel.automessage.dto.message.MessageFormEntry;
import excel.automessage.dto.message.MessageListDTO;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.entity.Members;
import excel.automessage.entity.Role;
import excel.automessage.entity.Store;
import excel.automessage.repository.MembersRepository;
import excel.automessage.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.FlashMap;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
class StoreControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    private static MockHttpSession session;

    @BeforeAll
    @DisplayName("API 테스트 로그인")
    static void setUp(@Autowired MembersRepository membersRepository,
                      @Autowired BCryptPasswordEncoder encoder,
                      @Autowired MockMvc mockMvc) throws Exception {

        membersRepository.deleteAll();

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

    }

    @BeforeEach
    @DisplayName("가게 테스트 더미 데이터 초기화")
    void setUp() {

        storeRepository.deleteAll();

        Store store = new Store("테스트가게", "01012341234");

        Store save = storeRepository.save(store);

        log.info("BeforeEach Store value = {}, {}", save.getStoreName(), store.getStorePhoneNumber());

    }

    // 가게 등록 성공(직접 등록 API)
    @Test
    @Transactional
    @DisplayName("가게 등록 성공(직접 등록 API)")
    void createStoreAPI() throws Exception {

        StoreListDTO storeList = new StoreListDTO();

        StoreDTO store1 = StoreDTO.builder()
                .name("홍길동")
                .phone("010-2345-6789")
                .build();

        StoreDTO store2 = StoreDTO.builder()
                .name("김모이")
                .phone("01098765432")
                .build();

        storeList.getStores().add(store1);
        storeList.getStores().add(store2);

        mockMvc.perform(post("/automessage/new/store")
                        .session(session)
                        .param("stores[0].name", storeList.getStores().get(0).getName())
                        .param("stores[0].phone", storeList.getStores().get(0).getPhone())
                        .param("stores[1].name", storeList.getStores().get(1).getName())
                        .param("stores[1].phone", storeList.getStores().get(1).getPhone())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/new/store"));

    }

    // 가게 수정 성공, 실패
    @Test
    @Transactional
    @DisplayName("가게 정보 수정 성공")
    void changeStoreValue() throws Exception {

        Optional<Store> findStore = storeRepository.findByStoreName("테스트가게");

        Long storeId = null;
        if (findStore.isPresent()) {
            storeId = findStore.get().getStoreId();
        }

        mockMvc.perform(post("/automessage/stores/{id}", storeId)
                        .session(session)
                        .param("id", String.valueOf(storeId))
                        .param("name", "테스트가게")
                        .param("phone", "01098765432")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/stores"));

        Optional<Store> result = storeRepository.findById(storeId);
        assertEquals(result.get().getStorePhoneNumber(), "01098765432");
    }

    @Test
    @Transactional
    @DisplayName("가게 정보 수정 실패 (전화번호 공백, 이름 공백, 전화번호 입력 값 오류)")
    void changeFailStoreValue() throws Exception {

        Optional<Store> findStore = storeRepository.findByStoreName("테스트가게");

        Long storeId = null;
        if (findStore.isPresent()) {
            storeId = findStore.get().getStoreId();
        }

        mockMvc.perform(post("/automessage/stores/{id}", storeId)
                        .session(session)
                        .param("id", String.valueOf(storeId))
                        .param("name", "테스트가게")
                        .param("phone", "010987654321")
                        .with(csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(redirectedUrl(null));

        mockMvc.perform(post("/automessage/stores/{id}", storeId)
                        .session(session)
                        .param("id", String.valueOf(storeId))
                        .param("name", "테스트가게")
                        .param("phone", "010")
                        .with(csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(redirectedUrl(null));

        mockMvc.perform(post("/automessage/stores/{id}", storeId)
                        .session(session)
                        .param("id", String.valueOf(storeId))
                        .param("name", "")
                        .param("phone", "010987654321")
                        .with(csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(redirectedUrl(null));


        mockMvc.perform(post("/automessage/stores/{id}", storeId)
                        .session(session)
                        .param("id", String.valueOf(storeId))
                        .param("name", "")
                        .param("phone", "")
                        .with(csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(redirectedUrl(null));

    }

    // 가게 삭제 성공
    @Test
    @Transactional
    @DisplayName("가게 삭제 성공")
    void deleteStoreSuccess() throws Exception {

        Optional<Store> findStore = storeRepository.findByStoreName("테스트가게");

        Long storeId = null;
        if (findStore.isPresent()) {
            storeId = findStore.get().getStoreId();
        }

        mockMvc.perform(post("/automessage/store/{id}", storeId)
                        .session(session)
                        .param("id", String.valueOf(storeId))
                        .param("category", "all")
                        .param("query", "")
                        .param("storeName", findStore.get().getStoreName())
                        .param("storePhoneNumber", findStore.get().getStorePhoneNumber())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/stores?category=all&query="));

    }

    @Test
    @Transactional
    @DisplayName("가게 삭제 실패 form data 다름")
    void deleteStoreFail() throws Exception {

        Optional<Store> findStore = storeRepository.findByStoreName("테스트가게");

        String userName = "잘못된 이름";
        String userPhone = "잘못된 번호";

        MvcResult result = mockMvc.perform(post("/automessage/store/{id}", findStore.get().getStoreId())
                        .session(session)
                        .param("id", String.valueOf(findStore.get().getStoreId()))
                        .param("category", "all")
                        .param("query", "")
                        .param("storeName", userName)
                        .param("storePhoneNumber", userPhone)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/stores?category=all&query="))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("errorMessage")).isEqualTo("잘못된 정보가 들어왔습니다");

    }

    // 가게 조회 검색
    @Test
    @Transactional
    @DisplayName("가게 목록 조회 성공")
    void getStore() throws Exception {

        mockMvc.perform(get("/automessage/stores?")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(redirectedUrl(null));

    }

    // 가게 검색 성공
    @Test
    @Transactional
    @DisplayName("가게 검색 성공")
    void searchStoreSuccess() throws Exception {

        mockMvc.perform(get("/automessage/stores?category={category}&query={query}", "all", "테스트")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(redirectedUrl(null));

    }

    @Test
    @Transactional
    @DisplayName("가게 검색 실패 (잘못된 카테고리 유형)")
    void searchStore_FailCategory() throws Exception {

        MvcResult result = mockMvc.perform(get("/automessage/stores?category={category}&query={query}", "234", "테스트")
                .session(session)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/stores"))
                .andReturn();

        FlashMap flashMap = result.getFlashMap();
        assertThat(flashMap.get("errorMessage")).isEqualTo("잘못된 카테고리 유형");

    }

    // 미등록 가게 성공
    @Test
    @Transactional
    @DisplayName("미등록 가게 성공")
    void missStoreCreateSuccess() throws Exception {

        StoreListDTO storeListDTO = new StoreListDTO();

        StoreDTO store = StoreDTO.builder()
                .name("홍길동")
                .phone("010-2345-6789")
                .build();

        storeListDTO.getStores().add(store);

        // 세션에 저장할 객체를 메시지 폼으로 변경
        MessageListDTO messageListDTO = new MessageListDTO();

        // 미등록 데이터
        MessageFormEntry entry = new MessageFormEntry();
        entry.setMissingStores(Arrays.asList("홍길동")); // 미등록 가게 이름
        messageListDTO.getMessageListDTO().add(entry);
        session.setAttribute("messageForm", messageListDTO);

        mockMvc.perform(post("/automessage/store/miss")
                        .session(session)
                        .with(csrf())
                        .param("stores[0].name", storeListDTO.getStores().get(0).getName())
                        .param("stores[0].phone", storeListDTO.getStores().get(0).getPhone()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/automessage/message/content"));
    }

}