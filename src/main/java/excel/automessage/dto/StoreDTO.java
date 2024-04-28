package excel.automessage.dto;

import excel.automessage.domain.Store;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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
                    .storePhoneNumber(phone)
                    .build();
        }
    }

}
