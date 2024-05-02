package excel.automessage.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter @Setter
public class SmsFormDTO {

    HashMap<String, List<String>> smsForm = new HashMap<>();
    HashMap<String, String> smsPhone = new HashMap<>();

}
