package excel.automessage.dto.message;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class MessageDTO {

    String to;
    String content;
    String storeName;
    List<String> productName;

}
