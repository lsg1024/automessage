package excel.automessage.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
public class HomeController {

    @RequestMapping("/")
    public String home() {
        log.info("home controller");
        return "home";
    }

    @RequestMapping("/store")
    public String store() {
        log.info("store controller");
        return "storeForm/storeSelect";
    }

    @RequestMapping("/store/excelStore")
    public String excelStore() {
        log.info("excelStore controller");
        return "storeForm/storeUpload";
    }


    @RequestMapping("/store/inputStore")
    public String inputStore() {
        log.info("inputStore controller");
        return "storeForm/storeInput";
    }

    @RequestMapping("/store/missingStore")
    public String missingStore() {
        log.info("missingStore Controller");
        return "storeForm/missingStore";
    }

    @RequestMapping("/storeList")
    public String storeList() {
        log.info("storeList controller");
        return "storeForm/storeList";
    }

//    @RequestMapping("storeList/edit")
//    public String storeEdit() {
//        log.info("storeEdit controller");
//        return "storeForm/storeUpdate";
//    }

    @RequestMapping("/sms")
    public String sms() {
        log.info("sms controller");
        return "smsForm/smsUpload";
    }

    @RequestMapping("/sms/content")
    public String smsContent() {
        log.info("smsContent Controller");
        return "smsForm/smsSendForm";
    }

}
