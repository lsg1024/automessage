package excel.automessage.dto.members;

import excel.automessage.entity.Members;
import excel.automessage.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class MembersDTO {

    @NotBlank(message = "입력이 잘못되었습니다")
    private String memberId;

    @NotBlank(message = "입력이 잘못되었습니다")
    private String memberPassword;

}
