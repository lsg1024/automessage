package excel.automessage.dto.members;

import excel.automessage.entity.Members;
import excel.automessage.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@NoArgsConstructor
@Getter @Setter
public class MembersDTO {

    @NotBlank(message = "입력이 잘못되었습니다")
    private String memberId;

    @NotBlank(message = "입력이 잘못되었습니다")
    private String memberPassword;
    private BCryptPasswordEncoder encoder;

    @Builder
    public MembersDTO(String memberId, String memberPassword) {
        this.memberId = memberId;
        this.memberPassword = memberPassword;
    }

    public Members toEntity() {
        return Members.builder()
                .memberId(memberId)
                .memberPassword(encoder.encode(memberPassword))
                .role(Role.WAIT)
                .build();
    }

}
