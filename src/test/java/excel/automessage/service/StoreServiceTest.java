package excel.automessage.service;

import excel.automessage.domain.Store;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class StoreServiceTest {

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreRepository storeRepository;

    // 가게 등록 (직접 등록) 성공, 실패
    @Transactional
    @DisplayName("가게 등록 성공(직접등록)")
    @Test
    void createStore() {
        //given
        StoreListDTO storeListDTO = new StoreListDTO();

        StoreDTO store_1 = new StoreDTO();
        store_1.setName("홍길동");
        store_1.setPhone("010-1234-5678");

        StoreDTO store_2 = new StoreDTO();
        store_2.setName("김모이");
        store_2.setPhone("01098765432");

        storeListDTO.getStores().add(store_1);
        storeListDTO.getStores().add(store_2);

        //when
        StoreListDTO saveResult = storeService.saveAll(storeListDTO);

        //then
        Optional<Store> saveStore_1 = storeRepository.findById(saveResult.getStores().get(0).getId());
        assertTrue(saveStore_1.isPresent());
        assertEquals(store_1.getName(), saveStore_1.get().getStoreName());
        assertEquals("01012345678", saveStore_1.get().getStorePhoneNumber());

        Optional<Store> saveStore_2 = storeRepository.findById(saveResult.getStores().get(1).getId());
        assertTrue(saveStore_2.isPresent());
        assertEquals(store_2.getName(), saveStore_2.get().getStoreName());
        assertEquals("01098765432", saveStore_2.get().getStorePhoneNumber());

    }

    @Transactional
    @DisplayName(("가게 등록 실패(직접등록)"))
    @Test
    void createFailStore() {
        //given
        StoreListDTO storeListDTO = new StoreListDTO();

        StoreDTO store_1 = new StoreDTO();
        store_1.setName("홍길동");
        store_1.setPhone("010/1234/5667");

        StoreDTO store_2 = new StoreDTO();
        store_2.setName("김모이");
        store_2.setPhone("010 9876 5432");

        storeListDTO.getStores().add(store_1);
        storeListDTO.getStores().add(store_2);

        //when
        //then
        assertThrows(IllegalStateException.class, () -> {
            storeService.saveAll(storeListDTO);
        }, "옳바른 번호를 입력해주세요.");

    }

    // 가게 등록 (엑셀 등록) 성공, 실패

    // 가게 수정 성공, 실패

    // 가게 삭제 성공, 실패

    // 가게 검색 성공, 실패

}
