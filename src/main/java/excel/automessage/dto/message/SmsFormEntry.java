package excel.automessage.dto.message;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class SmsFormEntry {
    public Map<String, String> phone;
    public Map<String, List<String>> smsForm;
    public List<String> missingStores;
    public String content;
    public boolean sendSms;

    public SmsFormEntry() {
        this.phone = new HashMap<>();
        this.smsForm = new HashMap<>();
        this.missingStores = new ArrayList<>();
    }
}
