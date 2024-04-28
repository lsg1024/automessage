package excel.automessage;

import org.springframework.util.StringUtils;

public class JavaTest {

    public static void main(String[] args) {
        String phone_1 = "010-1234-5678";
        String phone_2 = "02-1234-1234";

        System.out.println(StringUtils.startsWithIgnoreCase(phone_1, "010"));
        System.out.println(StringUtils.startsWithIgnoreCase(phone_2, "010"));
    }

}
