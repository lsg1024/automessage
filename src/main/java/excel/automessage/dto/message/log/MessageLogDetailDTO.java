package excel.automessage.dto.message.log;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

public class MessageLogDetailDTO {

    @Getter @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageLogsDTO {
        HashMap<String, List<String>> messageLogs;
    }

    @Getter @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageLogDTO {
        String storeName;
        String productName;
    }

}
