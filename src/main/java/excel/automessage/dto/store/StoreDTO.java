package excel.automessage.dto.store;

import excel.automessage.domain.Store;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoreDTO {

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