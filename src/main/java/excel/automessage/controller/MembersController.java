package excel.automessage.controller;

import excel.automessage.dto.members.MembersDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Slf4j
public class MembersController {

    @GetMapping("/")
    public String root() {
        log.info("root controller");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        log.info("login Page Controller");
        model.addAttribute("loginForm", new MembersDTO());
        return "membersForm/loginPage";
    }

    @PostMapping("/login")
    public String login(@Validated @ModelAttribute("loginForm") MembersDTO membersDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        log.info("login Controller Controller");

        return null;
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        log.info("signup Page Controller");
        model.addAttribute("signupForm", new MembersDTO());
        return "membersForm/signupPage";
    }

    @PostMapping("/signup")
    public String signup(@Validated @ModelAttribute("signupForm") MembersDTO membersDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        log.info("signup Controller");
        return null;
    }

}
