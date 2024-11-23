package excel.automessage.service.store.validate;

public class StoreValidate {

    public static void existStoreName(boolean existingStoreName, String storeName) {
        if (existingStoreName) {
            throw new IllegalArgumentException(storeName + "은 이미 존재하는 이름 입니다.");
        }
    }

    public static void existStorePhoneNumber(boolean existingStorePhoneNumber, String phoneNumber) {
        if (existingStorePhoneNumber) {
            throw new IllegalArgumentException(phoneNumber + " 이미 존재하는 번호 입니다");
        }
    }

}
