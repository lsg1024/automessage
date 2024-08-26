package excel.automessage.service.store;

import excel.automessage.BaseTest;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.entity.Store;
import excel.automessage.repository.MembersRepository;
import excel.automessage.repository.StoreRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@AutoConfigureMockMvc
public class StoreServiceTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Validator validator;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private MembersRepository membersRepository;

    @BeforeEach
    @DisplayName("가게 테스트 더미 데이터 초기화")
    void setUp() {
        Store store = new Store("테스트가게", "01012341234");

        Store save = storeRepository.save(store);

        log.info("BeforeEach Store value = {}, {}", save.getStoreName(), store.getStorePhoneNumber());

    }

    // 가게 등록 (직접 등록) 성공, 실패
    @Test
    @DisplayName("가게 등록 성공(직접등록)")
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

    @DisplayName(("가게 등록 실패(직접등록)"))
    @Test
    void createFailStore() {
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


    // 가게 등록 (엑셀 등록) 성공, 실패
//    @Test
//    @DisplayName("엑셀 가게 등록")
//    void createExcelStore() {
//
//    }


    // 가게 수정 성공, 실패

    // 가게 삭제 성공

    // 가게 검색 성공, 실패

}
