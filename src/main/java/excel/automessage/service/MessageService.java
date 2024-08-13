package excel.automessage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel.automessage.entity.MessageHistory;
import excel.automessage.entity.MessageStorage;
import excel.automessage.entity.Store;
import excel.automessage.dto.message.*;
import excel.automessage.repository.MessageStorageRepository;
import excel.automessage.repository.StoreRepository;
import excel.automessage.util.ExcelSheetUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
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

    // 메시지 정보 업로드
    @Transactional
    public ProductDTO.ProductList messageUpload(MultipartFile file) throws IOException {

        Workbook workbook = ExcelSheetUtils.getSheets(file);

        Sheet worksheet = workbook.getSheetAt(0);

        ProductDTO.ProductList productList = new ProductDTO.ProductList();

        extractedProductAndName(worksheet, productList);

        return productList;
    }

    // 메시지 내역
    @Transactional
    public SmsFormDTO messageForm(ProductDTO.ProductList productList) {
        SmsFormDTO smsFormDTO = new SmsFormDTO();
        List<SmsFormEntry> entries = new ArrayList<>();

        for (ProductDTO product : productList.getProductDTOList()) {
            SmsFormEntry smsFormEntry = new SmsFormEntry();
            String storeName = product.getStoreName();
            String productName = product.getProductName();

            // 기본값 설정
            smsFormEntry.setContent("안녕하세요 종로 칸입니다.\n오늘 물품이 내려갑니다.\n내일 통상 확인해주세요~"); // 기본값 설정, 필요에 따라 수정
            smsFormEntry.setSendSms(true);

            // 제품 목록에 추가
            smsFormEntry.getSmsForm().put(storeName, new ArrayList<>());
            smsFormEntry.getSmsForm().get(storeName).add(productName);

            Optional<Store> phoneNumber = storeRepository.findByStoreName(storeName);
            searchProductPhone(smsFormEntry, product, phoneNumber);

            entries.add(smsFormEntry);
        }

        smsFormDTO.setSmsFormDTO(entries);

        return smsFormDTO;
    }

    // 메시지 전송 여부 전처리
    @Transactional
    public List<MessageResponseDTO> processAndSendMessages(SmsFormDTO smsForm, List<Integer> errorMessage) {
        List<MessageDTO> messageDTOList = new ArrayList<>();

        for (int i = 0; i < smsForm.getSmsFormDTO().size(); i++) {
            SmsFormEntry entry = smsForm.getSmsFormDTO().get(i);


            if (entry.getPhone() == null) {
                log.error("전화 번호가 없음: {}", entry);
                errorMessage.add(i + 1); // 오류 메시지에 인덱스를 추가
                continue;
            }

            if (!entry.sendSms) {
                log.info("전송 요청 거부 {}", entry.getPhone());
                errorMessage.add(i + 1); // 오류 메시지에 인덱스를 추가
                continue;
            }

            log.info("Processing phone map: {}", entry.getPhone());

            for (Map.Entry<String, String> phoneEntry : entry.getPhone().entrySet()) {
                MessageDTO messageDTO = MessageDTO.builder()
                        .to(phoneEntry.getValue())
                        .content(entry.getContent())
                        .build();
                messageDTOList.add(messageDTO);
            }
        }

        log.info("sendMessage phone {}", smsForm.getSmsFormDTO().get(0).getPhone());

        return messageSend(messageDTOList, errorMessage);
    }

    // 메시지 전송
    @Transactional
    public List<MessageResponseDTO> messageSend(List<MessageDTO> messageDTOList, List<Integer> errorMessage) {
        List<MessageResponseDTO> responses = new ArrayList<>();
        List<MessageHistory> messageHistories = new ArrayList<>();

        log.info("messageSend Service");

        log.info("messageSend Service {}", messageDTOList.get(0).getTo());

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
        MessageResponseDTO response = restTemplate.postForObject(new URI("https://sens.apigw.ntruss.com/sms/v2/services/"+ serviceId +"/messages"), httpBody, MessageResponseDTO.class);

        return response;
    }

    // 메시지 로그 전체 조회

    // 미등록 가게 번호 검색
    private void searchProductPhone(SmsFormEntry smsFormEntry, ProductDTO product, Optional<Store> phoneNumber) {
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
    private void extractedProductAndName(Sheet worksheet, ProductDTO.ProductList productList) {

        DataFormatter dataFormatter = new DataFormatter();

        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            Row row = worksheet.getRow(i);
            if (row == null) continue;

            ProductDTO productDTO = new ProductDTO();

            Cell cell = row.getCell(11); // 판매 정보
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

    // 숫자만 있는지 확인
    private static boolean isNumber(String str) {
        return str.chars().allMatch(Character::isDigit);
    }

    // 메시지 로그
    private MessageHistory createMessageHistory(MessageDTO messageDTO, String status, String errorMessage) {
        return MessageHistory.builder()
                .receiver(messageDTO.getTo())
                .content(messageDTO.getContent())
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

}



