package excel.automessage.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDTO {

    Long id;

    @NotBlank(message = "이름 입력")
    String name;

    @Pattern(regexp = "^01[01]-?\\d{4}-?\\d{4}$", message = "잘못된 번호")
    String phone;

    @Getter @Setter
    @AllArgsConstructor
    public static class Update {

        @NotNull(message = "가게 정보를 찾을 수 없습니다")
        Long id;

        @NotBlank(message = "이름 입력 필수")
        String name;

        @NotBlank(message = "잘못된 입력")
        @Pattern(regexp = "^(010\\d{8})?$", message = "")
        String phone;

    }

}

