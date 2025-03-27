package excel.automessage.service.message;

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
import excel.automessage.service.message.util.MessageUtil;
import excel.automessage.service.message.util.NaverApi;
import excel.automessage.util.ExcelSheetUtils;
import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static excel.automessage.service.message.util.MessageUtil.extractedOrderExcelData;
import static excel.automessage.service.message.util.MessageUtil.isCheckboxSelected;

@Slf4j
@Service
@Timed("otalk.message")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    @Value("${MESSAGE_CONTENT}")
    private String message;

    private final StoreRepository storeRepository;
    private final MessageStorageRepository messageStorageRepository;
    private final MessageHistoryRepository messageHistoryRepository;
    private final NaverApi naverApi;

    // 메시지 정보 업로드
    @Transactional
    public ProductDTO.ProductList messageUpload(MultipartFile file, boolean option) throws IOException {

        Workbook workbook = ExcelSheetUtils.getSheets(file);

        Sheet worksheet = workbook.getSheetAt(0);

        ProductDTO.ProductList productList = new ProductDTO.ProductList();

        extractedOrderExcelData(worksheet, productList, option);

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
            log.info("금일 전송 번호 = {}", messageFormEntry.getPhone().entrySet());
        }

        return messageListDTO;
    }

    // 메시지 전송
    @Transactional
    public List<MessageResponseDTO> checkMessageSend(MessageListDTO messageListDTO, List<Integer> errorMessage) {
        log.info("checkMessageTransmission Service");
        List<MessageDTO> messageDTOList = new ArrayList<>();

        // 메시지 전송 여부 구분
        isCheckboxSelected(messageListDTO, messageDTOList);

        return messageSend(messageDTOList, errorMessage);
    }

    // 메시지 로그 전체 조회
    @Transactional
    public Page<MessageStorageDTO> searchMessageLog(String end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        log.info("searchMessageLog = {}", page);

        return messageStorageRepository.findByLastModifiedDateAll(end, pageable)
                .map(messageStorage -> new MessageStorageDTO(
                        messageStorage.getMessageStorageId(),
                        messageStorage.getLastModifiedDate().substring(0, 10)
                ));
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

    // 메시지 전송
    private List<MessageResponseDTO> messageSend(List<MessageDTO> messageDTOList, List<Integer> errorMessage) {
        List<MessageResponseDTO> responses = new ArrayList<>();
        List<MessageHistory> messageHistories = new ArrayList<>();

        // 전송 승인된 메시지 전송 및 에러 처리
        approvedMessageSend(messageDTOList, errorMessage, responses, messageHistories);

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

            Store store = storeRepository.findByStoreName(storeName).orElse(null);
            MessageUtil.searchProductPhone(smsFormEntry, product, store);
        }
    }

    private void approvedMessageSend(List<MessageDTO> messageDTOList, List<Integer> errorMessage, List<MessageResponseDTO> responses, List<MessageHistory> messageHistories) {
        for (int i = 0; i < messageDTOList.size(); i++) {
            MessageDTO messageDTO = messageDTOList.get(i);
            if (!MessageUtil.isOnlyNumber(messageDTO.getTo())) {
                log.error("전화번호 형식 오류로 인한 메시지 전송 실패 {}", messageDTO.getTo());
                errorMessage.add(i + 1);
                messageHistories.add(createMessageHistory(messageDTO, "전송 실패", "잘못된 전화번호 입니다."));
                continue;
            }

            try {
                MessageResponseDTO response = naverApi.requestNaverSmsApi(messageDTO);
                responses.add(response);
                messageHistories.add(createMessageHistory(messageDTO, "전송 성공", null));
                log.info("메시지 전송 성공 {}", messageDTO.getStoreName());
            } catch (Exception e) {
                errorMessage.add(i + 1);
                messageHistories.add(createMessageHistory(messageDTO, "전송 실패", e.getMessage()));
                log.error("메시지 전송 실패 {}, {}", messageDTO.getStoreName(), e.getMessage());
            }
        }

        log.info("messageSendingLogic finish");
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
}



