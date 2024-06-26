package excel.automessage.service;

import excel.automessage.domain.Store;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.repository.StoreRepository;
import excel.automessage.util.ExcelSheetUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {

    private final StoreRepository storeRepository;

    //가게 저장
    public StoreListDTO saveAll(StoreListDTO storeListDTO) {

        StoreListDTO result = new StoreListDTO();

        for (StoreDTO storeDTO : storeListDTO.getStores()) {

            //- 제거
            storeDTO.setPhone(removeHyphens(storeDTO.getPhone()));
            //번호 유효성 검사
            validateStoreNumber(storeDTO);

            Optional<Store> existingStore = storeRepository.findByStoreName(storeDTO.getName());
            Store store;
            if (existingStore.isPresent()) {
                // Store가 이미 존재하는 경우 업데이트
                store = existingStore.get();
                store.setStorePhoneNumber(storeDTO.getPhone());
                storeRepository.save(store);
            } else {
                // Store가 존재하지 않는 경우 새로 저장
                store = storeDTO.toEntity();
                storeRepository.save(store);
            }

            StoreDTO savedStoreDTO = new StoreDTO(store.getStoreId(), store.getStoreName(), store.getStorePhoneNumber());
            result.getStores().add(savedStoreDTO);
        }

        return result;
    }

    //가게 검색
    public Page<Store> searchStores(String category, String storeName, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        log.info("searchStores category = {}", category);
        log.info("searchStores storeName = {}", storeName);
        log.info("searchStores page = {}", page);
        log.info("searchStores size = {}", size);
        return storeRepository.findByCategoryAndStoreName(category, storeName, pageable);
    }

    //가게 수정
    public void updateStore(Long storeId, StoreDTO storeDTO) {

        log.info("store.getId() = {}", storeDTO.getId());
        log.info("store.getPhone() = {}", storeDTO.getPhone());

        Store store = findById(storeId);

        store.setStorePhoneNumber(storeDTO.getPhone());

        storeRepository.save(store);
    }

    //가게 삭제
    public void deleteStore(Long storeId) {
        storeRepository.deleteById(storeId);
    }

    public Store findById(Long id) {
        return storeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("가게 정보가 없습니다."));
    }

    //가게 저장 (엑셀)
    public StoreListDTO saveStores(MultipartFile file) throws IOException {

        Workbook workbook = ExcelSheetUtils.getSheets(file);

        Sheet worksheet = workbook.getSheetAt(0);
        StoreListDTO storeListDTO = new StoreListDTO();

        extractedNameAndPhone(worksheet, storeListDTO);

        workbook.close();

        return storeListDTO;
    }

    //번호 유효성 검사
    private void validateStoreNumber(StoreDTO storeDTO) {
        if (!storeDTO.getPhone().matches("\\d{10,11}")) {
            throw new IllegalStateException("옳바른 번호를 입력해주세요.");
        }
    }

    //하이픈 제거
    private String removeHyphens(String phoneNumber) {
        if (phoneNumber != null) {
            return phoneNumber.replaceAll("-", ""); // 모든 하이픈 제거
        }
        else {
            return null;
        }
    }

    //이름 번호 엑셀에서 추출
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
