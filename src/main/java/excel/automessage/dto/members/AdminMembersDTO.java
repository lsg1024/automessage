package excel.automessage.dto.members;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AdminMembersDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Members {
        private Long userId;
        private String memberId;
        private String memberRole;

    }

}
