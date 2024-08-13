package excel.automessage.dto.message;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class SmsFormDTO {
    private List<SmsFormEntry> smsFormDTO = new ArrayList<>();
}

