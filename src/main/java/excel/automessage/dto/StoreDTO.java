package excel.automessage.dto;

import excel.automessage.domain.Store;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class StoreDTO {

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Save {
        Long id;
        String name;
        String phone;

        public Store toEntity() {
            return Store.builder()
                    .storeName(name)
                    .storePhoneNumber(removeHyphens(phone))
                    .build();
        }
        private String removeHyphens(String phoneNumber) {
            if (phoneNumber != null) {
                return phoneNumber.replaceAll("-", ""); // 모든 하이픈 제거
            }
            else {
                return null;
            }

        }
    }

}
