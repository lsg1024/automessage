package excel.automessage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {

    private final StoreRepository storeRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveAll(StoreListDTO storeListDTO) {

        for (StoreDTO.Save storeDTO : storeListDTO.getStores()) {
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

    public Page<Store> searchStores(String category, String storeName, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        log.info("searchStores category = {}", category);
        log.info("searchStores storeName = {}", storeName);
        log.info("searchStores page = {}", page);
        log.info("searchStores size = {}", size);
        return storeRepository.findByCategoryAndStoreName(category, storeName, pageable);
    }


    public void updateStore(Long storeId, StoreDTO storeDTO) {

        log.info("store.getId() = {}", storeDTO.getId());
        log.info("store.getPhone() = {}", storeDTO.getPhone());

        Store store = findById(storeId);

        store.setStorePhoneNumber(storeDTO.getPhone());

        storeRepository.save(store);
    }

    public void deleteStore(Long storeId) {
        storeRepository.deleteById(storeId);
    }

    public Store findById(Long id) {
        return storeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("가게 정보가 없습니다."));
    }

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

    @Async
    public CompletableFuture<Void> saveAllToDBAsync(StoreListDTO storeListDTO, String key) {
        return CompletableFuture.runAsync(() -> {
            String serializedData = (String) redisTemplate.opsForValue().get(key);
            if (serializedData != null) {
                saveAll(storeListDTO);
                redisTemplate.delete(key);
            }
        });

    }


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

    private void extractedNameAndPhone(Sheet worksheet, StoreListDTO storeListDTO) {

        DataFormatter dataFormatter = new DataFormatter();

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
    }

}
