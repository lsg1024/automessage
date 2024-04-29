package excel.automessage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    String storeName;
    String productName;

    @Getter @Setter
    public static class ProductList {
        private List<ProductDTO> productDTOList;

        public ProductList() {
            productDTOList = new ArrayList<>(); // 생성자에서 리스트 초기화
        }

        public void addProduct(ProductDTO product) {
            productDTOList.add(product);
        }
    }

}
