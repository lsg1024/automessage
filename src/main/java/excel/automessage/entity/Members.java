package excel.automessage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Members  {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String memberId;
    private String memberPassword;
    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder
    public Members(String memberId, String memberPassword, Role role) {
        this.memberId = memberId;
        this.memberPassword = memberPassword;
        this.role = role;
    }

}
