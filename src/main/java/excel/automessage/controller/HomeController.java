package excel.automessage.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class HomeController {

    // 홈 화면
    @GetMapping("/automessage")
    public String mainPage() {
        log.info("mainPage controller");
        return "home";
    }

}
