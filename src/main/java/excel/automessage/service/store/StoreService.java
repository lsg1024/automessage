package excel.automessage.service.store;

import excel.automessage.dto.message.MessageFormEntry;
import excel.automessage.dto.message.MessageListDTO;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.entity.Store;
import excel.automessage.repository.StoreRepository;
import excel.automessage.excel.util.ExcelSheetUtils;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static excel.automessage.service.store.validate.StoreValidate.existStoreName;
import static excel.automessage.service.store.validate.StoreValidate.existStorePhoneNumber;

@Service
@Slf4j
@Timed("otalk.store")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    // 데이터 저장
    @Transactional
    public StoreListDTO saveAll(StoreListDTO storeListDTO) {

        StoreListDTO result = new StoreListDTO();

        saveStores(storeListDTO, result);

        return result;
    }

    // 가게 검색
    @Transactional
    public Page<Store> searchStores(String category, String storeName, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        log.info("searchStores category = {} storeName = {} page = {} size {}", category, storeName, page, size);

        if (!category.equals("") && !category.equals("all") && !category.equals("null") && !category.equals("notnull")) {
            throw new IllegalArgumentException("잘못된 카테고리 유형");
        }

        return storeRepository.findByCategoryAndStoreName(category, storeName, pageable);
    }

    // 가게 정보 업데이트
    @Transactional
    public StoreDTO.Update updateStore(Long storeId, StoreDTO.Update storeDTO) {

        log.info("store.getId() = {} store.getName = {} store.getPhone() = {}", storeDTO.getId(), storeDTO.getName(), storeDTO.getPhone());

        Store store = findById(storeId);

        boolean existsByStoreNameAndStoreIdNot = storeRepository.existsByStoreNameAndStoreIdNot(storeDTO.getName(), storeId);
        boolean existsByStorePhoneNumberAndStoreIdNot = storeRepository.existsByStorePhoneNumberAndStoreIdNot(storeDTO.getPhone(), storeId);

        existStoreName(existsByStoreNameAndStoreIdNot, storeDTO.getName());
        existStorePhoneNumber(existsByStorePhoneNumberAndStoreIdNot, storeDTO.getPhone());

        store.setStoreNameAndPhoneNumber(storeDTO.getName(), storeDTO.getPhone());

        storeRepository.save(store);

        return storeDTO;
    }

    // 가게 삭제
    @Transactional
    public void deleteStore(Long storeId, String storeName, String storePhoneNumber) {
        log.info("가게 삭제 완료 id = {}", storeId);
        Store findStore = findById(storeId);

        if (!findStore.getStoreName().equals(storeName) || !findStore.getStorePhoneNumber().equals(storePhoneNumber)) {
            throw new IllegalArgumentException("잘못된 정보가 들어왔습니다");
        }

        storeRepository.deleteById(storeId);
    }

    // 가게 데이터 찾기
    @Transactional
    public Store findById(Long id) {
        return storeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("가게 정보가 없습니다."));
    }

    // 가게 저장 (엑셀)
    public StoreListDTO saveExcelStores(MultipartFile file) throws IOException {

        Workbook workbook = ExcelSheetUtils.getSheets(file);

        Sheet worksheet = workbook.getSheetAt(0);
        StoreListDTO storeListDTO = new StoreListDTO();

        extractedNameAndPhone(worksheet, storeListDTO);

        workbook.close();

        return storeListDTO;
    }

    // 미등록 가게
    @Transactional
    public MessageListDTO saveMissingStore(StoreListDTO storeListDTO, MessageListDTO messageListDTO) {
        StoreListDTO result = new StoreListDTO();

        for (StoreDTO saveStore : storeListDTO.getStores()) {
            log.info("saveMissingStore name {}, number {}", saveStore.getName(), saveStore.getPhone());

            // 전화번호에서 하이픈 제거
            saveStore.setPhone(removeHyphens(saveStore.getPhone()));
            log.info("after number {}", saveStore.getPhone());

            // 새로운 스토어를 저장
            Store store = storeRepository.save(Store.builder()
                    .storeName(saveStore.getName())
                    .storePhoneNumber(saveStore.getPhone())
                    .build());

            // 저장된 스토어 정보를 DTO로 변환하여 결과 리스트에 추가
            StoreDTO savedStoreDTO = StoreDTO.builder()
                    .id(store.getStoreId())
                    .name(store.getStoreName())
                    .phone(store.getStorePhoneNumber())
                    .build();

            result.getStores().add(savedStoreDTO);
        }

        // MissingStores에서 해당 값을 찾아 phone에 추가
        updateMissingStoresWithPhone(messageListDTO, storeListDTO);

        return messageListDTO;
    }

    // 미등록 가게 데이터 해당 index phone에 삽입
    private void updateMissingStoresWithPhone(MessageListDTO messageListDTO, StoreListDTO storeListDTO) {

        // 전체 데이터 크기
        for (int i = 0; i < messageListDTO.getMessageListDTO().size(); i++) {
            MessageFormEntry entry = messageListDTO.getMessageListDTO().get(i);

            // 미등록 데이터 검색
            for (String missingStore : entry.getMissingStores()) {

                // 미등록 데이터와 일치하면 해당 map에 phone 정보 넣기 missing : [홍길동:01012345678] -> getPhone[홍길동:01012345678]
                for (StoreDTO storeDTO : storeListDTO.getStores()) {
                    if (storeDTO.getName().equals(missingStore)) {
                        entry.getPhone().put(storeDTO.getName(), storeDTO.getPhone());
                        break;
                    }
                }
            }
        }
    }

    private void saveStores(StoreListDTO storeListDTO, StoreListDTO result) {
        for (StoreDTO saveStore : storeListDTO.getStores()) {

            log.info("saveStores name {}, number {}", saveStore.getName(), saveStore.getPhone());

            //- 제거
            saveStore.setPhone(removeHyphens(saveStore.getPhone()));

            boolean existsByStoreName = storeRepository.existsByStoreName(saveStore.getName());
            boolean existByStorePhoneNumber = storeRepository.existsByStorePhoneNumber(saveStore.getPhone());

            if (existsByStoreName || existByStorePhoneNumber) {
                log.error("중복된 이름 혹은 전화번호가 포함되었습니다.");
                continue;
            }

            Store store = Store.builder()
                    .storeName(saveStore.getName())
                    .storePhoneNumber(saveStore.getPhone())
                    .build();
            storeRepository.save(store);

            StoreDTO savedStoreDTO = StoreDTO.builder()
                    .id(store.getStoreId())
                    .name(store.getStoreName())
                    .phone(store.getStorePhoneNumber())
                    .build();

            result.getStores().add(savedStoreDTO);
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
