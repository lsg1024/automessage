package excel.automessage.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class HomeController {

    // 홈 화면
    @GetMapping("/automessage")
    public String mainPage(Model model, HttpSession session) {
        log.info("mainPage controller");

        String message = (String) session.getAttribute("loginSuccess");
        if (message != null) {
            model.addAttribute("success", message);
            log.info("message success = {}", message);
            session.removeAttribute("loginSuccess");
        }

        return "home";
    }

    @GetMapping("/loginSuccess")
    public String loginSuccess(HttpSession httpSession) {
        httpSession.setAttribute("loginSuccess", "로그인 성공");
        return "redirect:/automessage";
    }

}
