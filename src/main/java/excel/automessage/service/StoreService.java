package excel.automessage.service;

import excel.automessage.domain.Store;
import excel.automessage.dto.store.StoreDTO;
import excel.automessage.dto.store.StoreListDTO;
import excel.automessage.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    // 데이터 저장
    @Transactional
    public void saveAll(StoreListDTO storeListDTO) {

        for (StoreDTO storeDTO : storeListDTO.getStores()) {
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
        }

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
    public void updateStore(Long storeId, StoreDTO storeDTO) {

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

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        Workbook workbook = null;

        if (extension == null) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }

        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (NotOfficeXmlFileException e) {
            workbook = convertHtmlToWorkbook(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (workbook == null) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        Sheet worksheet = workbook.getSheetAt(0);
        StoreListDTO storeListDTO = new StoreListDTO();

        extractedNameAndPhone(worksheet, storeListDTO);

        workbook.close();

        return storeListDTO;
    }

    // html 테이블 읽기
    private Workbook convertHtmlToWorkbook(MultipartFile htmlFile) throws IOException {
        Document htmlDoc = Jsoup.parse(htmlFile.getInputStream(), "UTF-8", "");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        Element table = htmlDoc.select("table").first();
        if (table != null) {
            Elements rows = table.select("tr");

            int rowIndex = 0;
            for (Element row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                Elements cells = row.select("td, th");
                int cellIndex = 0;
                for (Element cell : cells) {
                    Cell excelCell = excelRow.createCell(cellIndex++);
                    excelCell.setCellValue(cell.text());
                }
            }
        }

        return workbook;
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
