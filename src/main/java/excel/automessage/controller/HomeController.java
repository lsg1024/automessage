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
