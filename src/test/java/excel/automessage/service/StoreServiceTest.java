package excel.automessage.service;

import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.entity.Store;
import excel.automessage.repository.StoreRepository;
import excel.automessage.service.store.StoreService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class StoreServiceTest {

    @Autowired
    private Validator validator;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreRepository storeRepository;

    // 가게 등록 (직접 등록) 성공, 실패
    @Transactional
    @DisplayName("가게 등록 성공(직접등록)")
    @Test
    void createStore() {
        // given
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

        // when
        Set<ConstraintViolation<StoreListDTO>> violations = validator.validate(storeList);

        // then
        List<String> messages = new ArrayList<>();
        for (ConstraintViolation<StoreListDTO> violation : violations) {
            messages.add(violation.getMessage());
            log.info("message = {}", violation.getMessage());
        }

        // 검증 오류가 없어야 한다
        assertTrue(violations.isEmpty(), "검증 오류가 없어야 합니다.");

        // 유효한 데이터로 저장을 시도한다
        StoreListDTO saveResult = storeService.saveAll(storeList);

        log.info("getID = {}", saveResult.getStores().get(0).getId());
        Optional<Store> saveStore_1 = storeRepository.findById(saveResult.getStores().get(0).getId());
        assertTrue(saveStore_1.isPresent());
        assertEquals(store1.getName(), saveStore_1.get().getStoreName());
        assertEquals("01023456789", saveStore_1.get().getStorePhoneNumber());

        Optional<Store> saveStore_2 = storeRepository.findById(saveResult.getStores().get(1).getId());
        assertTrue(saveStore_2.isPresent());
        assertEquals(store2.getName(), saveStore_2.get().getStoreName());
        assertEquals("01098765432", saveStore_2.get().getStorePhoneNumber());

    }

//    @Transactional
//    @DisplayName(("가게 등록 실패(직접등록)"))
//    @Test
//    void createFailStore() {
//        //given
//        StoreListDTO storeListDTO = new StoreListDTO();
//
//        StoreDTO store_1 = StoreDTO.builder()
//                .name("홍길동")
//                .phone("010/1234/5667")
//                .build();
//
//        StoreDTO store_2 = StoreDTO.builder()
//                .name("김모이")
//                .phone("010 9876 5432")
//                .build();
//
//        storeListDTO.getStores().add(store_1);
//        storeListDTO.getStores().add(store_2);
//
//        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
//            storeService.saveAll(storeListDTO);
//        });
//
//        assertEquals("올바른 번호를 입력해주세요.", exception.getMessage());
//
//    }

    // 가게 등록 (엑셀 등록) 성공, 실패

    // 가게 수정 성공, 실패

    // 가게 삭제 성공, 실패

    // 가게 검색 성공, 실패

}
