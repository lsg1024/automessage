package excel.automessage.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class HomeController {

    // 홈 화면
    @GetMapping("/automessage")
    public String mainPage(Model model) {
        log.info("mainPage controller");

        model.addAttribute("success", "로그인 성공");

        return "home";
    }

}
