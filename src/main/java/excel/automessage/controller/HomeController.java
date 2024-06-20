package excel.automessage.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
public class HomeController {

    // 홈 화면
    @RequestMapping("/")
    public String home() {
        log.info("home controller");
        return "home";
    }

    // 새로운 상점 url
    @RequestMapping("/new")
    public String store() {
        log.info("store controller");
        return "storeForm/storeSelect";
    }

    // 새로운 상점 (엑셀 입력)
    @RequestMapping("/new/stores")
    public String excelStore() {
        log.info("excelStore controller");
        return "storeForm/storeUpload";
    }

    // 새로운 상점 (직접 입력)
    @RequestMapping("/new/store")
    public String newStore() {
        log.info("newStore controller");
        return "storeForm/storeInput";
    }
//
//    // 추가 상점 (미등록 상점)
//    @RequestMapping("/store/add")
//    public String addStore() {
//        log.info("addStore Controller");
//        return "storeForm/missingStore";
//    }
//
//    @RequestMapping("/stores")
//    public String storeList() {
//        log.info("stores controller");
//        return "storeForm/storeList";
//    }
//
//    @RequestMapping("/sms")
//    public String sms() {
//        log.info("sms controller");
//        return "smsForm/smsUpload";
//    }
//
    @RequestMapping("/sms/content")
    public String smsContent() {
        log.info("smsContent Controller");
        return "smsForm/smsSendForm";
    }

}
