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
        return "storeForm/storeUpload";
    }

    @RequestMapping("/sms")
    public String sms() {
        log.info("sms controller");
        return "messageForm/messageUpload";
    }

    @RequestMapping("/sms/content")
    public String smsContent() {
        log.info("smsContent Controller");
        return "messageForm/messageList";
    }

}
