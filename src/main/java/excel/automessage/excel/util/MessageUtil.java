package excel.automessage.excel.util;

import excel.automessage.dto.message.MessageDTO;
import excel.automessage.dto.message.MessageFormEntry;
import excel.automessage.dto.message.MessageListDTO;
import excel.automessage.dto.message.ProductDTO;
import excel.automessage.entity.Store;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class MessageUtil {

    public static boolean isOnlyNumber(String number) {
        return number.chars().allMatch(Character::isDigit);
    }

    public static void searchProductPhone(MessageFormEntry smsFormEntry, ProductDTO product, Store store) {
        if (store != null) {
            String phone = store.getStorePhoneNumber();
            smsFormEntry.getPhone().put(product.getStoreName(), Objects.requireNonNullElse(phone, "번호 없음"));
        } else {
            // 미등록 가게 추가
            smsFormEntry.getMissingStores().add(product.getStoreName());
        }
    }

    /**
     * html 메시지 전송 여부 체크 박스 메서드
     * @param messageListDTO 프론트 전송데이터
     * @param messageDTOList 백엔드 정제 데이터
     */
    public static void isCheckboxSelected(MessageListDTO messageListDTO, List<MessageDTO> messageDTOList) {
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

            for (Map.Entry<String, String> phoneEntry : entry.getPhone().entrySet()) {
                MessageDTO messageDTO = MessageDTO.builder()
                        .to(phoneEntry.getValue())
                        .content(entry.getContent())
                        .storeName(phoneEntry.getKey())
                        .productName(productNames)
                        .build();
                messageDTOList.add(messageDTO);

                log.info("isCheckMessageSending phone Key = {}", phoneEntry.getKey());
            }
        }
    }

    /**
     * 엑셀 데이터 정제 메서드
     * @param productList 상품 데이터
     * @param option 크롤링 서버 자동 다운 여부 체크
     */
    public static void extractedOrderExcelData(Sheet worksheet, ProductDTO.ProductList productList, boolean option) {

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


}
