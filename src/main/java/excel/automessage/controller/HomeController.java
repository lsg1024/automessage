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
    public String storeExcelStore() {
        log.info("store controller");
        return "storeForm/storeUpload";
    }


    @RequestMapping("/store/inputStore")
    public String storeInputStore() {
        log.info("store controller");
        return "storeForm/storeInput";
    }


    @RequestMapping("/sms")
    public String sms() {
        log.info("sms controller");
        return "smsForm/smsUpload";
    }

    @RequestMapping("/sms/content")
    public String smsContent() {
        log.info("smsContent Controller");
        return "smsForm/smsList";
    }

}
