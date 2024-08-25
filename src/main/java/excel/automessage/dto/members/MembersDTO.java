package excel.automessage.dto.members;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class MembersDTO {

    @NotBlank(message = "입력이 잘못되었습니다")
    private String memberId;

    @NotBlank(message = "입력이 잘못되었습니다")
    private String memberPassword;

}
