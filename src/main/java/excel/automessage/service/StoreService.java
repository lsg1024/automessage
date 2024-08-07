package excel.automessage.service;

import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.entity.Store;
import excel.automessage.repository.StoreRepository;
import excel.automessage.util.ExcelSheetUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final Validator validator;

    // 데이터 저장
    @Transactional
    public StoreListDTO saveAll(StoreListDTO storeListDTO) {

        StoreListDTO result = new StoreListDTO();

        for (StoreDTO saveStore : storeListDTO.getStores()) {

            Store store;

            log.info("storeDto name {}, number {}", saveStore.getName(), saveStore.getPhone());

            //- 제거
            saveStore.setPhone(removeHyphens(saveStore.getPhone()));

            log.info("saveStore.getPhone() = {}", saveStore.getPhone());

            //번호 유효성 검사 - validation
//            validateStoreNumber(saveStore);

            Optional<Store> existingStore = storeRepository.findByStoreName(saveStore.getName());

            if (existingStore.isPresent()) {
                // Store가 이미 존재하는 경우 업데이트
                store = existingStore.get();
                store.setStorePhoneNumber(saveStore.getPhone());
                storeRepository.save(store);
            } else {
                // Store가 존재하지 않는 경우 새로 저장
                store = Store.builder()
                        .storeName(saveStore.getName())
                        .storePhoneNumber(saveStore.getPhone())
                        .build();

                storeRepository.save(store);
            }

            StoreDTO savedStoreDTO = StoreDTO.builder()
                    .id(store.getStoreId())
                    .name(store.getStoreName())
                    .phone(store.getStorePhoneNumber())
                    .build();

            result.getStores().add(savedStoreDTO);
        }

        return result;
    }

    // 가게 검색
    @Transactional
    public Page<Store> searchStores(String category, String storeName, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        log.info("searchStores category = {} storeName = {} page = {} size {}", category, storeName, page, size);

        return storeRepository.findByCategoryAndStoreName(category, storeName, pageable);
    }

    // 가게 정보 업데이트
    @Transactional
    public void updateStore(Long storeId, StoreDTO.Update storeDTO) {

        log.info("store.getId() = {} store.getPhone() = {}", storeDTO.getId(), storeDTO.getPhone());

        Store store = findById(storeId);

        store.setStorePhoneNumber(storeDTO.getPhone());

        storeRepository.save(store);
    }

    // 가게 삭제
    @Transactional
    public void deleteStore(Long storeId) {
        log.info("가게 삭제 완료 id = {}", storeId);
        storeRepository.deleteById(storeId);
    }

    // 가게 데이터 찾기
    @Transactional
    public Store findById(Long id) {
        return storeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("가게 정보가 없습니다."));
    }

    // 가게 저장 (엑셀)
    public StoreListDTO saveStores(MultipartFile file) throws IOException {

        Workbook workbook = ExcelSheetUtils.getSheets(file);

        Sheet worksheet = workbook.getSheetAt(0);
        StoreListDTO storeListDTO = new StoreListDTO();

        extractedNameAndPhone(worksheet, storeListDTO);

        workbook.close();

        return storeListDTO;
    }


    //번호 유효성 검사
    private void validateStoreNumber(StoreDTO saveStore) {
        log.info("validateStoreNumber {}", saveStore.getPhone());

        if (saveStore.getPhone() == null) {
            return;
        }
        if (!saveStore.getPhone().matches("\\d{10,11}")) {
            throw new IllegalStateException("올바른 번호를 입력해주세요.");
        }
    }

    //하이픈 제거
    private String removeHyphens(String phoneNumber) {
        log.info("removeHyphens after {}", phoneNumber);
        if (phoneNumber != null) {
            return phoneNumber.replaceAll("-", ""); // 모든 하이픈 제거
        }
        else {
            return null;
        }

    }


    // 가게 저장 데이터 포멧 (엑셀)
    private void extractedNameAndPhone(Sheet worksheet, StoreListDTO storeListDTO) {

        DataFormatter dataFormatter = new DataFormatter();

        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            Row row = worksheet.getRow(i);
            if (row == null) continue;

            StoreDTO data = new StoreDTO();

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
    }

}
