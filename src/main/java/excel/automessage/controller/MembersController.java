package excel.automessage.controller;

import excel.automessage.dto.members.MembersDTO;
import excel.automessage.service.MembersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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
@RequiredArgsConstructor
public class MembersController {

    private final MembersService membersService;

    @GetMapping("/")
    public String root() {
        log.info("root controller");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(Model model, Authentication authentication) {
        log.info("login Page Controller");

        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/automessage";
        }

        model.addAttribute("loginForm", new MembersDTO());
        return "membersForm/loginPage";
    }

    @PostMapping("/login")
    public String login(@Validated @ModelAttribute("loginForm") MembersDTO membersDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        log.info("login Controller Controller");

        if (bindingResult.hasErrors()) {
            return "membersForm/loginPage";
        }

        return "redirect:/automessage";
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

        if (bindingResult.hasErrors()) {
            return "membersForm/signupPage";
        }

        try {
            membersService.createUser(membersDTO);
            redirectAttributes.addFlashAttribute("message", "회원 가입 성공 승인 대기 중");
            return "redirect:/login";
        } catch (Exception e) {
            bindingResult.reject("signupFail", "회원가입 오류 발생");
            return "membersForm/signupPage";
        }

    }

}
