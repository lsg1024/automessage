package excel.automessage.controller;

import excel.automessage.dto.members.MembersDTO;
import excel.automessage.service.member.MembersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
    public String loginPage(@ModelAttribute("response") String response, Model model, HttpServletRequest request, Authentication authentication) {
        log.info("login Page Controller");

        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/automessage";
        }

        HttpSession session = request.getSession();
        String errorMessage = (String) session.getAttribute("errorMessage");

        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            session.removeAttribute("errorMessage");
        }

        log.info("loginPage response {}", response);

        model.addAttribute("loginForm", new MembersDTO());
        model.addAttribute("message", response);
        return "membersForm/loginPage";
    }

    @PostMapping("/login")
    public String login(@Validated @ModelAttribute("loginForm") MembersDTO membersDTO, BindingResult bindingResult) {
        log.info("login Controller Controller");

        if (bindingResult.hasErrors()) {
            return "redirect:/login";
        }

        return "redirect:/automessage";
    }

    @GetMapping("/signup")
    public String signupPage(@ModelAttribute("response") String response, Model model) {
        log.info("signup Page Controller");
        model.addAttribute("signupForm", new MembersDTO());
        model.addAttribute("response", response);
        return "membersForm/signupPage";
    }

    @PostMapping("/signup")
    public String signup(@Validated @ModelAttribute("signupForm") MembersDTO membersDTO, BindingResult bindingResult, RedirectAttributes redirectAttributes, HttpServletResponse response) {
        log.info("signup Controller");

        if (bindingResult.hasErrors()) {
            response.setStatus(400);
            return "membersForm/signupPage";
        }

        try {
            Boolean member = membersService.createMember(membersDTO);

            log.info("member bool {}", member);
            if (member) {
                redirectAttributes.addFlashAttribute("response", "회원 가입 성공 승인 대기 중");
            }
            else {
                redirectAttributes.addFlashAttribute("response", "이미 가입된 아이디 입니다.");
                return "redirect:/signup";
            }
            return "redirect:/login";
        } catch (Exception e) {
            bindingResult.reject("signupFail", "회원가입 오류 발생");
            return "membersForm/signupPage";
        }

    }

}
