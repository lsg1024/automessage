package excel.automessage.service;

import excel.automessage.BaseTest;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.entity.Store;
import excel.automessage.repository.StoreRepository;
import excel.automessage.service.store.StoreService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class StoreServiceTest extends BaseTest {

    @Autowired
    private Validator validator;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreRepository storeRepository;

    @BeforeAll
    @DisplayName("가게 테스트 더미 데이터 초기화")
    static void setUp(@Autowired StoreRepository storeRepository) {

        Store store1 = Store.builder()
                .storeName("테스트 가게 1")
                .storePhoneNumber("01012341234")
                .build();

        Store store2 = Store.builder()
                .storeName("테스트 가게 2")
                .storePhoneNumber("01099998888")
                .build();

        Store save1 = storeRepository.save(store1);
        Store save2 = storeRepository.save(store2);

        Store save_store1 = storeRepository.findById(save1.getStoreId()).orElse(null);
        Store save_store2 = storeRepository.findById(save2.getStoreId()).orElse(null);

        assertNotNull(save_store1);
        assertNotNull(save_store2);

        log.info("BeforeEach save_store1 value = {}, {}", save_store1.getStoreName(), save_store1.getStorePhoneNumber());
        log.info("BeforeEach save_store2 value = {}, {}", save_store2.getStoreName(), save_store2.getStorePhoneNumber());

    }

    // 가게 등록 validation Test
    @Test
    @DisplayName("가게 등록 성공(DTO Validation)")
    void createStoreDto() {
        //given
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

        //when
        Set<ConstraintViolation<StoreListDTO>> violations = validator.validate(storeList);

        //then
        List<String> messages = new ArrayList<>();
        for (ConstraintViolation<StoreListDTO> violation : violations) {
            messages.add(violation.getMessage());
            log.info("message = {}", violation.getMessage());
        }

        // 검증 오류가 없어야 한다
        assertTrue(violations.isEmpty(), "검증 오류가 없어야 합니다.");

    }

    @DisplayName(("가게 등록 실패(DTO Validation)"))
    @Test
    void createFailStoreDto() {
        //given
        StoreListDTO storeListDTO = new StoreListDTO();

        //부족한 숫자
        StoreDTO store_1 = StoreDTO.builder()
                .name("홍길동")
                .phone("0101234123")
                .build();

        //공백
        StoreDTO store_2 = StoreDTO.builder()
                .name("김모이")
                .phone("010 9876 5432")
                .build();

        //허용되지 않은 특수 문자
        StoreDTO store_3 = StoreDTO.builder()
                .name("테스트")
                .phone("010/9876/5432")
                .build();

        storeListDTO.getStores().add(store_1);
        storeListDTO.getStores().add(store_2);
        storeListDTO.getStores().add(store_3);

        //when
        Set<ConstraintViolation<StoreListDTO>> violations = validator.validate(storeListDTO);

        //then
        List<String> messages = new ArrayList<>();
        for (ConstraintViolation<StoreListDTO> violation : violations) {
            messages.add(violation.getMessage());
            assertEquals(violation.getMessage(), "잘못된 번호");
        }

    }

    @Test
    @DisplayName("가게 등록 성공(직접등록)")
    @Transactional
    void createStore() {

        //given
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

        //when
        StoreListDTO result = storeService.saveAll(storeList);

        //then
        assertEquals(result.getStores().get(0).getName(), storeList.getStores().get(0).getName());
        assertEquals(result.getStores().get(1).getName(), storeList.getStores().get(1).getName());

    }

    @Test
    @DisplayName("가게 검색")
    @Transactional
    void searchStore() {

        //given
        Store store = Store.builder()
                .storeName("가게 검색용")
                .storePhoneNumber("01044445555")
                .build();

        storeRepository.save(store);

        List<Store> all = storeRepository.findAll();
        for (Store store1 : all) {
            log.info("store data = {}", store1.getStoreName());
        }

        //when
        Page<Store> searchResult = storeService.searchStores("all", "", 0, 10);

        //then
        assertEquals(searchResult.getTotalPages(), 1);
        assertEquals(searchResult.getContent().size(), 3);
        assertEquals(searchResult.getContent().get(2).getStoreName(), store.getStoreName());

    }

    // 가게 수정 성공
    @Test
    @DisplayName("가게 정보 수정 성공")
    @Transactional
    void storeUpdateSuccess() {

        //given
        Store store = Store.builder()
                .storeName("가게 정보 수정 전")
                .storePhoneNumber("01098765432")
                .build();

        Store saveStore = storeRepository.save(store);

        //when
        StoreDTO.Update updateStore = new StoreDTO.Update(saveStore.getStoreId(), "가게 정보 수정 후", "01023456789");
        StoreDTO.Update result = storeService.updateStore(saveStore.getStoreId(), updateStore);

        Store updatedStore = storeRepository.findById(saveStore.getStoreId()).orElseThrow();
        log.info("result.getName() = {}, store.getStoreName = {}", result.getName(), updatedStore.getStoreName());
        //then
        assertEquals(result.getName(), updatedStore.getStoreName());
        assertEquals(result.getPhone(), updatedStore.getStorePhoneNumber());

    }

    @Test
    @DisplayName("가게 정보 수정 실패 (pk NULL)")
    @Transactional
    void storeUpdateFail_pk_null() {

        //given
        Long wrongId = 99999L;
        StoreDTO.Update update = new StoreDTO.Update(wrongId, "새로운 가게", "010123456789");

        //when & then
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            storeService.updateStore(wrongId, update);
        });

    }

//    @Test
//    @DisplayName("가게 번호 중복 체크")
//    @Transactional
//    void storeUpdate_CheckSamePhoneNumber() {
//
//    }

    // 가게 삭제 성공
    @Test
    @DisplayName("가게 정보 삭제 성공")
    @Transactional
    void storeDeleteSuccess() {

        //given
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

        StoreListDTO result = storeService.saveAll(storeList);

        //when
        storeService.deleteStore(result.getStores().get(0).getId(), result.getStores().get(0).getName(), result.getStores().get(0).getPhone());

        //then
        assertThrows(IllegalArgumentException.class, () -> {
            storeService.findById(result.getStores().get(0).getId());
        });

    }

    @Test
    @DisplayName("가게 이름 혹은 번호가 일치하지 않을 때 삭제 실패 (불일치 데이터)")
    @Transactional
    void storeDeleteFail_NotSameData() {

        //given
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

        StoreListDTO result = storeService.saveAll(storeList);

        //when && then
        assertThrows(IllegalArgumentException.class, () -> {
            storeService.deleteStore(result.getStores().get(0).getId(), result.getStores().get(1).getName(), result.getStores().get(1).getPhone());
        });
    }

    @Test
    @DisplayName("존재하지 않는 가게 ID로 삭제 시도 시 실패")
    @Transactional
    void deleteStoreWithNonExistentId_shouldFail() {
        // given
        Long wrongId = 999L; // 존재하지 않는 ID
        String storeName = "없는 가게";
        String storePhoneNumber = "01000000000";

        // when & then
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            storeService.deleteStore(wrongId, storeName, storePhoneNumber);
        });
    }

}
