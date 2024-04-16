package excel.automessage.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ResponseDTO {

    String result;
    List<String> message;

    public ResponseDTO(String result) {
        this.result = result;
    }

    public ResponseDTO(String result, List<String> message) {
        this.result = result;
        this.message = message;
    }
}
