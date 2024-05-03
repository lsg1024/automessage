package excel.automessage.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class StoreListDTO {
    private List<StoreDTO.Save> stores;

    public StoreListDTO() {
        this.stores = new ArrayList<>();
    }

}
