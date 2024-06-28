package excel.automessage.dto.message;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class MessageFormDTO {
    public Map<String, List<String>> smsForm;
    public Map<String, String> smsPhone;
    public List<String> missingStores;

    public MessageFormDTO() {
        this.smsForm = new HashMap<>();
        this.smsPhone = new HashMap<>();
        this.missingStores = new ArrayList<>();
    }
}
