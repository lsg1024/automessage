package excel.automessage.service;

import excel.automessage.domain.Store;
import excel.automessage.dto.StoreDTO;
import excel.automessage.dto.StoreListDTO;
import excel.automessage.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {

    private final StoreRepository storeRepository;

    public void saveAll(StoreListDTO storeListDTO) {

        for (StoreDTO.Save storeDTO : storeListDTO.getStores()) {
            Optional<Store> existingStore = storeRepository.findByStoreName(storeDTO.getName());
            if (existingStore.isPresent()) {
                // Store가 이미 존재하는 경우 업데이트
                Store store = existingStore.get();
                store.setStorePhoneNumber(storeDTO.getPhone());
                storeRepository.save(store);
            } else {
                // Store가 존재하지 않는 경우 새로 저장
                Store store = storeDTO.toEntity();
                storeRepository.save(store);
            }
        }

    }

    public Page<Store> searchStores(String category, String storeName, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        log.info("searchStores category = {}", category);
        log.info("searchStores storeName = {}", storeName);
        log.info("searchStores page = {}", page);
        log.info("searchStores size = {}", size);
        return storeRepository.findByCategoryAndStoreName(category, storeName, pageable);
    }

    public Store findById(Long id) {

        return storeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("가게 정보가 없습니다."));
    }

    public void updateStore(Store store) {
        storeRepository.save(store);
    }

    public StoreListDTO formattingValue(Sheet worksheet) {

        DataFormatter dataFormatter = new DataFormatter();

        StoreListDTO storeListDTO = new StoreListDTO();
        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            Row row = worksheet.getRow(i);
            if (row == null) continue;

            StoreDTO.Save data = new StoreDTO.Save();

            Cell cell = row.getCell(5); // 이름 셀
            if (cell != null) {
                data.setName(dataFormatter.formatCellValue(cell));
            }

            cell = row.getCell(16); // 전화번호 셀
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String phoneNumber = cell.getStringCellValue();
                if (phoneNumber.startsWith("010")) {
                    data.setPhone(phoneNumber);
                }
            }
            storeListDTO.getStores().add(data);

        }

        return storeListDTO;
    }

}
