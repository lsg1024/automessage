package excel.automessage.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class MessageDTO {

    String to;
    String content;

    public static class MessageLog {
        String messageId;
        String requestTime;
        String completeTime;
        String from;
        String to;
        String content;

    }
}
