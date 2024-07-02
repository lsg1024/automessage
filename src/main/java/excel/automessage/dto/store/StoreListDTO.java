package excel.automessage.dto.store;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class StoreListDTO {
    private List<StoreDTO> stores;

    public StoreListDTO() {
        this.stores = new ArrayList<>();
    }

}
