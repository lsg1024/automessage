package excel.automessage.dto.message;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class MessageListDTO {
    private List<MessageFormEntry> messageListDTO = new ArrayList<>();
}

