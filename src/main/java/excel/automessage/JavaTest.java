package excel.automessage;

import excel.automessage.dto.ProductDTO;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JavaTest {

    public static void main(String[] args) {
        String phone_1 = "010-1234-5678";
        String phone_2 = "02-1234-1234";

        System.out.println(StringUtils.startsWithIgnoreCase(phone_1, "010"));
        System.out.println(StringUtils.startsWithIgnoreCase(phone_2, "010"));

        String productName_1 = "통상비";
        String productName_2 = "통상";

        System.out.println(StringUtils.startsWithIgnoreCase(productName_1, "통상"));
        System.out.println(StringUtils.startsWithIgnoreCase(productName_2, "통상"));

        ProductDTO.ProductList productList = new ProductDTO.ProductList();
        productList.addProduct(new ProductDTO("대구보석나라", "SV223B-LQ"));
        productList.addProduct(new ProductDTO("대구보석나라", "1121"));
        productList.addProduct(new ProductDTO("대전귀금속", "KA791S"));
        productList.addProduct(new ProductDTO("대전귀금속", "2342"));

        for (int i = 0; i < productList.getProductDTOList().size(); i++) {
            System.out.println(productList.getProductDTOList().get(i).getStoreName());
            System.out.println(productList.getProductDTOList().get(i).getProductName());
        }

        HashMap<String, List<String>> hashMap = new HashMap<>();

        for (ProductDTO product : productList.getProductDTOList()) {
            // 맵에 해당 storeName이 이미 키로 존재하는지 확인합니다.
            List<String> products = hashMap.computeIfAbsent(product.getStoreName(), k -> new ArrayList<>());
            // storeName에 해당하는 productName 리스트에 추가합니다.
            products.add(product.getProductName());
        }

        hashMap.forEach((store, products) -> System.out.println(store + " => " + products));

//        System.out.println(productList.getProductDTOList().size());
//        for (int i = 0; i < productList.getProductDTOList().size(); i++) {
//
//            hashMap.(productList.getProductDTOList().get(i).getStoreName(), Collections.singletonList(productList.getProductDTOList().get(i).getProductName()));
//
//        }
//
//        System.out.println(hashMap);

    }

}
