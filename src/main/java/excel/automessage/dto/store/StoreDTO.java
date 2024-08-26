package excel.automessage.dto.store;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDTO {

    Long id;

    @NotBlank(message = "이름 입력")
    String name;

    @Pattern(regexp = "^[0-9-]{11,13}$", message = "잘못된 번호")
    String phone;

    @Getter @Setter
    @AllArgsConstructor
    public static class Update {

        @NotNull(message = "가게 정보를 찾을 수 없습니다")
        Long id;

        @NotBlank(message = "이름 입력 필수")
        String name;

        @Pattern(regexp = "^(01[01]\\d{8})?$", message = "")
        String phone;

    }

}

