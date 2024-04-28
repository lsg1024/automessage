package excel.automessage.dto;

import java.util.ArrayList;
import java.util.List;

public class StoreListDTO {
    private List<StoreDTO.Save> stores;

    public StoreListDTO() {
        this.stores = new ArrayList<>();
    }

    public List<StoreDTO.Save> getStores() {
        return stores;
    }

    public void setStores(List<StoreDTO.Save> stores) {
        this.stores = stores;
    }
}
