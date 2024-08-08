package excel.automessage.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    USER("USER", "일반유저"),
    WAIT("WAIT", "승인 대기"),
    ADMIN("ADMIN", "관리자");

    private final String key;
    private final String title;
}
