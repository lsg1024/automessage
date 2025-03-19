package excel.automessage.service.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel.automessage.dto.message.*;
import excel.automessage.dto.message.log.MessageLogDetail;
import excel.automessage.dto.message.log.MessageLogDetailDTO;
import excel.automessage.dto.message.log.MessageStorageDTO;
import excel.automessage.entity.MessageHistory;
import excel.automessage.entity.MessageStorage;
import excel.automessage.entity.ProductHistory;
import excel.automessage.entity.Store;
import excel.automessage.repository.MessageHistoryRepository;
import excel.automessage.repository.MessageStorageRepository;
import excel.automessage.repository.StoreRepository;
import excel.automessage.util.ExcelSheetUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MessageService {

    @Value("${naver-cloud-sms.accessKey}")
    private String accessKey;

    @Value("${naver-cloud-sms.secretKey}")
    private String secretKey;

    @Value("${naver-cloud-sms.serviceId}")
    private String serviceId;

    @Value("${naver-cloud-sms.senderPhone}")
    private String phone;

    private final StoreRepository storeRepository;
    private final MessageStorageRepository messageStorageRepository;
    private final MessageHistoryRepository messageHistoryRepository;

    // 메시지 정보 업로드
    @Transactional
    public ProductDTO.ProductList messageUpload(MultipartFile file, boolean option) throws IOException {

        Workbook workbook = ExcelSheetUtils.getSheets(file);

        Sheet worksheet = workbook.getSheetAt(0);

        ProductDTO.ProductList productList = new ProductDTO.ProductList();

        extractedProductAndName(worksheet, productList, option);

        return productList;
    }

    // 메시지 내역
    @Transactional
    public MessageListDTO messageForm(ProductDTO.ProductList productList) {
        MessageListDTO messageListDTO = new MessageListDTO();
        List<MessageFormEntry> entries = new ArrayList<>();

        // 가게 이름을 키로 사용하는 맵을 생성하여 중복을 방지
        Map<String, MessageFormEntry> smsFormEntryMap = new HashMap<>();

        // 메시지 기본 폼 전달
        BasicMessageForm(productList, entries, smsFormEntryMap);

        messageListDTO.setMessageListDTO(entries);

        for (MessageFormEntry messageFormEntry : messageListDTO.getMessageListDTO()) {
            log.info("entries = {}", messageFormEntry.getSmsForm().entrySet());
            log.info("entries phone = {}", messageFormEntry.getPhone().entrySet());
        }

        return messageListDTO;
    }

    // 메시지 전송 여부 전처리
    @Transactional
    public List<MessageResponseDTO> checkMessageTransmission(MessageListDTO messageListDTO, List<Integer> errorMessage) {
        log.info("checkMessageTransmission Service");
        List<MessageDTO> messageDTOList = new ArrayList<>();

        // 메시지 전송 여부 구분
        isCheckMessageSending(messageListDTO, messageDTOList);

        return messageSend(messageDTOList, errorMessage);
    }

    // 메시지 전송
    private List<MessageResponseDTO> messageSend(List<MessageDTO> messageDTOList, List<Integer> errorMessage) {
        List<MessageResponseDTO> responses = new ArrayList<>();
        List<MessageHistory> messageHistories = new ArrayList<>();

        log.info("messageSend Service");
        log.info("messageSend Service {}", messageDTOList.get(0).getTo());
        
        // 전송 승인된 메시지 전송 및 에러 처리
        messageSendingLogic(messageDTOList, errorMessage, responses, messageHistories);

        // 메시지 스토리지 초기화
        MessageStorage messageStorage = MessageStorage.builder()
                .messageHistories(new ArrayList<>())
                .build();

        // 메시지 내역 스토리지 매핑
        for (MessageHistory history : messageHistories) {
            messageStorage.addMessageHistory(history);
        }

        messageStorageRepository.save(messageStorage);

        return responses;
    }

    // 메시지 전송 양식 (네이버 sms)
    @Transactional
    public MessageResponseDTO messageSendForm(MessageDTO messageDto) throws JsonProcessingException, RestClientException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException {

        Long time = System.currentTimeMillis();
        String Sign = makeSignature(time);

        log.info("Sing : {}", Sign);
        log.info("time : {}", time);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-ncp-apigw-timestamp", time.toString());
        headers.set("x-ncp-iam-access-key", accessKey);
        headers.set("x-ncp-apigw-signature-v2", Sign);

        List<MessageDTO> messages = new ArrayList<>();
        messages.add(messageDto);

        MessageRequestDTO request = MessageRequestDTO.builder()
                .type("SMS")
                .contentType("COMM")
                .countryCode("82")
                .from(phone)
                .content(messageDto.getContent())
                .messages(messages)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String body = objectMapper.writeValueAsString(request);
        HttpEntity<String> httpBody = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        return restTemplate.postForObject(new URI("https://sens.apigw.ntruss.com/sms/v2/services/"+ serviceId +"/messages"), httpBody, MessageResponseDTO.class);
    }

    // 메시지 로그 전체 조회
    @Transactional
    public Page<MessageStorageDTO> searchMessageLog(String end, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        log.info("searchMessageLog = {}", page);
        Page<MessageStorage> messageLogPage = messageStorageRepository.findByLastModifiedDateAll(end, pageable);

        return messageLogPage.map(this::convertDto);

    }

    // 메시지 로그 상세조회
    @Transactional
    public MessageLogDetailDTO.MessageLogsDTO searchMessageLogDetail(String id) {

        List<MessageLogDetail> detailLogs = messageHistoryRepository.findDetailLog(id);

        HashMap<String, List<String>> messageLogs = new HashMap<>();

        for (MessageLogDetail detailLog : detailLogs) {
            String storeName = detailLog.getStoreName();
            String productName = detailLog.getProductName();

            List<String> products = messageLogs.getOrDefault(storeName, new ArrayList<>());
            products.add(productName);
            messageLogs.put(storeName, products);
        }

        return new MessageLogDetailDTO.MessageLogsDTO(messageLogs);
    }

    // 메시지 로그 삭제
    @Transactional
    public void deleteLog(String id) {
        MessageStorage messageStorage = messageStorageRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new EntityNotFoundException("잘못된 로그 아이디 입니다: " + id));

        messageStorageRepository.delete(messageStorage);
    }

    private void BasicMessageForm(ProductDTO.ProductList productList, List<MessageFormEntry> entries, Map<String, MessageFormEntry> smsFormEntryMap) {
        for (ProductDTO product : productList.getProductDTOList()) {
            String storeName = product.getStoreName();
            String productName = product.getProductName();

            MessageFormEntry smsFormEntry;

            // 가게 이름이 이미 존재하는지 확인
            if (smsFormEntryMap.containsKey(storeName)) {
                // 기존의 SmsFormEntry 가져오기
                smsFormEntry = smsFormEntryMap.get(storeName);
            } else {
                // 새로운 SmsFormEntry 생성
                smsFormEntry = new MessageFormEntry();
                smsFormEntry.setContent("안녕하세요 종로 칸입니다.\n오늘 물품이 내려갑니다.\n내일 통상 확인해주세요~"); // 기본값 설정, 필요에 따라 수정
                smsFormEntry.setSendSms(true);
                smsFormEntry.getSmsForm().put(storeName, new ArrayList<>());
                smsFormEntryMap.put(storeName, smsFormEntry);
                entries.add(smsFormEntry);
            }

            // 제품 이름 추가
            smsFormEntry.getSmsForm().get(storeName).add(productName);

            log.info("messageForm storeName {} {}", storeName, productName);

            Optional<Store> phoneNumber = storeRepository.findByStoreName(storeName);
            searchProductPhone(smsFormEntry, product, phoneNumber);
        }
    }

    // 미등록 가게 번호 검색
    private void searchProductPhone(MessageFormEntry smsFormEntry, ProductDTO product, Optional<Store> phoneNumber) {
        if (phoneNumber.isPresent()) {
            String phone = phoneNumber.get().getStorePhoneNumber();
            smsFormEntry.getPhone().put(product.getStoreName(), Objects.requireNonNullElse(phone, "번호 없음"));
        } else {
            // 미등록 가게 추가
            smsFormEntry.getMissingStores().add(product.getStoreName());
        }
    }

    // 메시지 암호화
    private String makeSignature(Long time) throws NoSuchAlgorithmException, InvalidKeyException {
        String space = " ";
        String newLine = "\n";
        String method = "POST";
        String url = "/sms/v2/services/"+ this.serviceId+"/messages";
        String timestamp = time.toString();
        String accessKey = this.accessKey;
        String secretKey = this.secretKey;

        String message = new StringBuilder()
                .append(method)
                .append(space)
                .append(url)
                .append(newLine)
                .append(timestamp)
                .append(newLine)
                .append(accessKey)
                .toString();

        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

        return Base64.encodeBase64String(rawHmac);
    }

    // 엑셀 데이터 포멧팅
    private void extractedProductAndName(Sheet worksheet, ProductDTO.ProductList productList, boolean option) {

        DataFormatter dataFormatter = new DataFormatter();

        Cell cell;
        Row row;
        LocalDate today = LocalDate.now();

        if (option) {
            row = worksheet.getRow(1);
            cell = row.getCell(3); //자동화 옵션의 경우 엑셀에서 날짜 체크
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String sellType = cell.getStringCellValue();
                if (!sellType.equals(today.toString())) {
                    throw new IllegalArgumentException("오늘 판매 데이터가 아닙니다.\n수동으로 입력해주세요.");
                }
            }
        }

        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            row = worksheet.getRow(i);
            if (row == null) continue;

            ProductDTO productDTO = new ProductDTO();

            cell = row.getCell(11); // 판매 정보
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String sellType = cell.getStringCellValue();
                if (!sellType.startsWith("판매")) {
                    continue;
                }
            }

            cell = row.getCell(14); // 상품 정보
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String productName = cell.getStringCellValue();
                if (!productName.startsWith("통상")) {
                    productDTO.setProductName(productName);
                } else {
                    continue;
                }
            }

            cell = row.getCell(9); // 이름 셀
            if (cell != null) {
                productDTO.setStoreName(dataFormatter.formatCellValue(cell));
            }

            productList.getProductDTOList().add(productDTO);
        }
    }

    private void messageSendingLogic(List<MessageDTO> messageDTOList, List<Integer> errorMessage, List<MessageResponseDTO> responses, List<MessageHistory> messageHistories) {
        for (int i = 0; i < messageDTOList.size(); i++) {
            MessageDTO messageDTO = messageDTOList.get(i);
            if (!isNumber(messageDTO.getTo())) {
                log.error("전화번호 형식 오류로 인한 메시지 전송 실패 {}", messageDTO.getTo());
                errorMessage.add(i + 1);
                messageHistories.add(createMessageHistory(messageDTO, "전송 실패", "잘못된 전화번호 입니다."));
                continue;
            }

            try {
                MessageResponseDTO response = messageSendForm(messageDTO);
                responses.add(response);
                messageHistories.add(createMessageHistory(messageDTO, "전송 성공", null));
                log.info("메시지 전송 성공 {}", messageDTO.getTo());
            } catch (Exception e) {
                errorMessage.add(i + 1);
                messageHistories.add(createMessageHistory(messageDTO, "전송 실패", e.getMessage()));
                log.error("메시지 전송 실패 {}, {}", messageDTO.getTo(), e.getMessage());
            }
        }

        log.info("messageSendingLogic finish");
    }
    
    // 숫자만 있는지 확인
    private static boolean isNumber(String str) {
        return str.chars().allMatch(Character::isDigit);
    }

    private static void isCheckMessageSending(MessageListDTO messageListDTO, List<MessageDTO> messageDTOList) {
        for (int i = 0; i < messageListDTO.getMessageListDTO().size(); i++) {
            MessageFormEntry entry = messageListDTO.getMessageListDTO().get(i);


            if (entry.getPhone() == null) {
                log.error("전화 번호가 없음: {}", entry);
                continue;
            }

            if (!entry.sendSms) {
                log.info("전송 요청 거부 {}", entry.getPhone());
                continue;
            }

            List<String> productNames = entry.getSmsForm().values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            log.info("processAndSend = {}", productNames);

            for (Map.Entry<String, String> phoneEntry : entry.getPhone().entrySet()) {
                MessageDTO messageDTO = MessageDTO.builder()
                        .to(phoneEntry.getValue())
                        .content(entry.getContent())
                        .storeName(phoneEntry.getKey())
                        .productName(productNames)
                        .build();
                messageDTOList.add(messageDTO);

                log.info("phone Key = {}", phoneEntry.getKey());
            }
        }
    }

    // 메시지 로그
    private MessageHistory createMessageHistory(MessageDTO messageDTO, String status, String errorMessage) {

        log.info("message getProductName = {}", messageDTO.getProductName().toString());

        List<ProductHistory> productHistoryList = messageDTO.getProductName().stream().map(productName ->
            ProductHistory.builder()
                    .productName(productName)
                    .build()
        ).toList();

        return MessageHistory.builder()
                .receiver(messageDTO.getTo())
                .content(messageDTO.getContent())
                .status(status)
                .errorMessage(errorMessage)
                .storeName(messageDTO.getStoreName())
                .productNames(productHistoryList)
                .build();
    }

    private MessageStorageDTO convertDto(MessageStorage messageStorage) {
        return new MessageStorageDTO(
                messageStorage.getMessageStorageId(),
                messageStorage.getLastModifiedDate().substring(0, 10)
        );
    }
}



