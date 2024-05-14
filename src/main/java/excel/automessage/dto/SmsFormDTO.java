package excel.automessage.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class SmsFormDTO {
    public Map<String, List<String>> smsForm;
    public Map<String, String> smsPhone;
    public List<String> missingStores;

    public SmsFormDTO() {
        this.smsForm = new HashMap<>();
        this.smsPhone = new HashMap<>();
        this.missingStores = new ArrayList<>();
    }
}
