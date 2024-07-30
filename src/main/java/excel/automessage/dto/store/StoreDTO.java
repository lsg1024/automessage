package excel.automessage.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDTO {

    Long id;

    @NotBlank(message = "사용자 이름을 입력해주세요.")
    String name;

    @Pattern(regexp = "^01(?:0|1|[0-1])[.-]?(\\d{3}|\\d{4})[.-]?(\\d{4})$", message = "10 ~ 11 자리의 숫자만 입력 가능합니다.")
    String phone;

}

