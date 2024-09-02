package excel.automessage.service;

import excel.automessage.BaseTest;
import excel.automessage.dto.message.MessageFormEntry;
import excel.automessage.dto.message.MessageListDTO;
import excel.automessage.dto.message.MessageResponseDTO;
import excel.automessage.dto.message.ProductDTO;
import excel.automessage.dto.message.log.MessageLogDetailDTO;
import excel.automessage.dto.message.log.MessageStorageDTO;
import excel.automessage.entity.MessageHistory;
import excel.automessage.entity.MessageStorage;
import excel.automessage.entity.ProductHistory;
import excel.automessage.repository.MessageHistoryRepository;
import excel.automessage.repository.MessageStorageRepository;
import excel.automessage.repository.ProductHistoryRepository;
import excel.automessage.service.message.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class MessageServiceTest extends BaseTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ProductHistoryRepository productHistoryRepository;

    @Autowired
    private MessageStorageRepository messageStorageRepository;

    @Autowired
    private MessageHistoryRepository messageHistoryRepository;

    private static MockMultipartFile file;

    @AfterEach
    void tearDown() {
        productHistoryRepository.deleteAll();
        messageHistoryRepository.deleteAll();
        messageStorageRepository.deleteAll();
    }

    @BeforeAll
    @DisplayName("테스트용 엑셀 더미 데이터 생성")
    static void setUp(@Autowired MessageStorageRepository messageStorageRepository,
                      @Autowired MessageHistoryRepository messageHistoryRepository) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // 첫 번째 행에 헤더 추가
        Row header = sheet.createRow(0);
        header.createCell(11).setCellValue("판매 정보");
        header.createCell(14).setCellValue("상품 정보");
        header.createCell(9).setCellValue("이름");

        // 두 번째 행에 데이터 추가
        Row row = sheet.createRow(1);
        row.createCell(11).setCellValue("판매");
        row.createCell(14).setCellValue("테스트용 상품 1");
        row.createCell(9).setCellValue("테스트 가게 1");

        // 세 번째 행에 올바른 데이터 추가
        row = sheet.createRow(2);
        row.createCell(11).setCellValue("판매");
        row.createCell(14).setCellValue("테스트용 상품 2");
        row.createCell(9).setCellValue("테스트 가게 2");

        // 엑셀 파일을 byte 배열로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // MockMultipartFile 생성
        file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", inputStream);

        workbook.close();

        // log 검색용 더미 데이터
        ProductHistory productHistory1 = ProductHistory.builder()
                .productName("Product 1")
                .build();
        ProductHistory productHistory2 = ProductHistory.builder()
                .productName("Product 2")
                .build();

        List<ProductHistory> productHistories = new ArrayList<>();
        productHistories.add(productHistory1);
        productHistories.add(productHistory2);

        // MessageHistory 더미 데이터 생성
        MessageHistory messageHistory1 = MessageHistory.builder()
                .receiver("010-1234-5678")
                .content("Test message content 1")
                .status("전송 성공")
                .errorMessage(null)
                .storeName("Store 1")
                .productNames(productHistories)
                .build();

        MessageHistory messageHistory2 = MessageHistory.builder()
                .receiver("010-9876-5432")
                .content("Test message content 2")
                .status("전송 실패")
                .errorMessage("Network error")
                .storeName("Store 2")
                .productNames(productHistories)
                .build();

        List<MessageHistory> messageHistories = new ArrayList<>();
        messageHistories.add(messageHistory1);
        messageHistories.add(messageHistory2);

        // MessageStorage 더미 데이터 생성 및 MessageHistory 추가
        MessageStorage messageStorage = MessageStorage.builder()
                .messageHistories(messageHistories)
                .build();

        // 각 MessageHistory에 MessageStorage 설정
        messageHistory1.setMessageStorage(messageStorage);
        messageHistory2.setMessageStorage(messageStorage);

        // 데이터 저장
        messageStorageRepository.save(messageStorage);

    }

    @Test
    @DisplayName("메시지 정보 업로드 성공")
    @Transactional
    void messageUploadSuccess() throws IOException {

        // 테스트할 메서드 호출
        ProductDTO.ProductList productList = messageService.messageUpload(file);

        // 결과 검증
        assertThat(productList.getProductDTOList()).hasSize(2);
        assertThat(productList.getProductDTOList().get(0).getProductName()).isEqualTo("테스트용 상품 1");
        assertThat(productList.getProductDTOList().get(0).getStoreName()).isEqualTo("테스트 가게 1");
    }

    @Test
    @DisplayName("메시지 정보 업로드 실패")
    @Transactional
    void messageUploadFail_type() throws IOException {

        //given
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // 데이터 추가 (형식 오류)
        Row row = sheet.createRow(1);
        row.createCell(11).setCellValue("잘못된 데이터");
        row.createCell(14).setCellValue("통상 상품");
        row.createCell(9).setCellValue("가게명");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", inputStream);

        //when
        ProductDTO.ProductList productList = messageService.messageUpload(file);

        //then
        assertThat(productList.getProductDTOList()).hasSize(0);
    }

    @Test
    @DisplayName("메시지 전송 여부 전처리")
    @Transactional
    void checkMessageTransmissionSuccess() {

        List<Integer> errorMessage = new ArrayList<>();

        MessageFormEntry messageFormEntry = new MessageFormEntry();
        Map<String, String> phone = new HashMap<>();
        phone.put("store1", "01012344321");
        messageFormEntry.setPhone(phone);

        Map<String, List<String>> smsFormMap = new HashMap<>();
        smsFormMap.put("store1", Arrays.asList("테스트 제품 1", "테스트 제품 2"));
        messageFormEntry.setSmsForm(smsFormMap);

        List<String> missingStoresList = List.of("");
        messageFormEntry.setMissingStores(missingStoresList);

        String content = "테스트 내용";
        messageFormEntry.setContent(content);

        boolean sendSms = true;
        messageFormEntry.setSendSms(sendSms);

        MessageListDTO messageListDTO = new MessageListDTO();
        messageListDTO.getMessageListDTO().add(messageFormEntry);

        List<MessageResponseDTO> response = messageService.checkMessageTransmission(messageListDTO, errorMessage);

        for (MessageResponseDTO messageResponseDTO : response) {
            log.info("messageResponseDTO = {}", messageResponseDTO.getRequestId());
            log.info("messageResponseDTO = {}", messageResponseDTO.getRequestTime());
            log.info("messageResponseDTO = {}", messageResponseDTO.getStatusCode());
            log.info("messageResponseDTO = {}", messageResponseDTO.getRequestTime());
        }

        // 외부 naver api를 사용해야됨..
//        assertEquals(response.get(0).getStatusCode(), 200);

    }

    // 메시지 로그 조회
    @Test
    @DisplayName("메시지 로그 조회 성공")
    @Transactional
    void messageLogSearchSuccess() {

        //given
        int size = 10;
        String end = LocalDateTime.now().toString().substring(0, 10) + " " + "23:59:59";

        //when
        Page<MessageStorageDTO> messageLog = messageService.searchMessageLog(end, 0, 10);

        int totalPage = messageLog.getTotalPages();
        int currentPage = messageLog.getNumber() + 1;
        int startPage = ((currentPage - 1) / size) * size + 1;
        int endPage = Math.min(startPage + size - 1, totalPage);

        //then
        assertEquals(totalPage, 1);
        assertEquals(currentPage, 1);
        assertEquals(startPage, 1);
        assertEquals(endPage, 1);

    }

    // 메시지 상세 페이지 조회
    @Test
    @DisplayName("메시지 상세 페이지 조회 성공")
    @Transactional
    void messageDetailLogSearchSuccess() {

        //given
        HashMap<String, List<String>> productList = new HashMap<>();
        List<String> products = new ArrayList<>();
        products.add("Product 1");
        products.add("Product 2");

        productList.put("Store 2", products);

        //when
        MessageLogDetailDTO.MessageLogsDTO detailLogs = messageService.searchMessageLogDetail("1");

        //then
        assertEquals(detailLogs.getMessageLogs().keySet(), productList.keySet());

    }

    @Test
    @DisplayName("메시지 상세 페이지 조회 실패 (null data)")
    @Transactional
    void messageDetailLogSearchFail() {

        //given
        HashMap<String, List<String>> messageLogs = new HashMap<>();

        //when
        MessageLogDetailDTO.MessageLogsDTO detailLogs = messageService.searchMessageLogDetail("2");

        //then
        assertEquals(detailLogs.getMessageLogs().keySet(), messageLogs.keySet());

    }

}