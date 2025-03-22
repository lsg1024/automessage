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

    @Column(unique = true)
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

    public void updateRole(String memberId, String role) {
        if (role.isEmpty()) {
            throw new IllegalArgumentException("권한 설정을 옳바르게 해주세요.");
        }

        if (!this.memberId.equals(memberId)) {
            throw new IllegalArgumentException("사용자 이름 변경은 불가능합니다.");
        }

        if (role.equals("USER") || role.equals("WAIT") || role.equals("ADMIN")) {
            this.role = Role.valueOf(role);
        }
        throw new IllegalArgumentException("잘못된 권한 설정 입니다.");
    }

}
