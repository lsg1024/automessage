package excel.automessage.dto.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class StoreListDTO {

    @Valid
    @NotEmpty(message = "새로운 가게 정보를 입력해주세요.")
    private List<StoreDTO> stores;

    public StoreListDTO() {
        this.stores = new ArrayList<>();
    }

}
