package excel.automessage.dto.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class StoreListDTO {

    @Valid
    @NotNull
    @Size(min = 1, message = "최소 한 개의 가게 정보를 입력해주세요.")
    private List<StoreDTO> stores = new ArrayList<>();

}
