package excel.automessage.dto.message;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class MessageRequestDTO {
    String type;
    String contentType;
    String countryCode;
    String from;
    String content;
    List<MessageDTO> messages;

}
