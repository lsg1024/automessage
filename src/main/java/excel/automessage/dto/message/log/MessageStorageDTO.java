package excel.automessage.dto.message.log;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageStorageDTO {

    private Long messageStorageId;
    private String messageStorageDate;

}
