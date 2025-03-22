package excel.automessage.dto.message;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class MessageListDTO {
    private List<MessageFormEntry> messageListDTO = new ArrayList<>();

    public void addStore(String storeName, String storePhoneNumber) {
        MessageFormEntry entry = new MessageFormEntry();
        entry.getPhone().put(storeName, storePhoneNumber);
        entry.content = "안녕하세요 종로 칸입니다.\n오늘 물품이 내려갑니다.\n내일 통상 확인해주세요~";
        entry.sendSms = true;

        messageListDTO.add(entry);
    }
}

