package excel.automessage.controller;

import excel.automessage.dto.members.AdminMembersDTO;
import excel.automessage.dto.members.MembersDTO;
import excel.automessage.entity.Members;
import excel.automessage.service.member.MembersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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
        log.info("login Controller");

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

    /**
     * 관리자 전용
     */
    @GetMapping("/error/access-denied")
    public String accessDeniedPage(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "관리자 아이디로 접속해주십시오.");
        return "redirect:/automessage";
    }

    @GetMapping("/automessage/admin")
    public String adminPage(
            @RequestParam(defaultValue = "1") int page,
            @ModelAttribute("adminForm") AdminMembersDTO membersDTO,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {
        log.info("admin Page Controller");

        int size = 10;

        Page<Members> membersPage = membersService.membersPage(page - 1, size, userDetails.getUsername());

        int totalPage = membersPage.getTotalPages();
        int currentPage = membersPage.getNumber() + 1;
        int startPage = ((currentPage - 1) / size) * size + 1;
        int endPage =  Math.min(startPage + size - 1, totalPage);

        if (page > totalPage) {
            redirectAttributes.addFlashAttribute("response", "유효하지 않은 페이지 입니다.");
            return "redirect:/automessage/admin";
        }

        model.addAttribute("url", "admin");
        model.addAttribute("membersPage", membersPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPage", totalPage);

        return "membersForm/adminPage";
    }

    @GetMapping("/automessage/admin/role/{id}")
    public String editMemberRole(@PathVariable("id") Long id, Model model) {
        log.info("editMemberRole Controller");

        AdminMembersDTO.Members members = membersService.findById(id);
        model.addAttribute("memberDTO", members);
        return "membersForm/rolePage";
    }

    @PostMapping("/automessage/admin/role/{id}")
    public String updateMemberRole(
            @PathVariable("id") Long id,
            @ModelAttribute("memberDTO") AdminMembersDTO.Members memberDTO,
            RedirectAttributes redirectAttributes) {

        try {
            membersService.adminUpdateRole(id, memberDTO);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("response", e.getMessage());
            return "redirect:/automessage/admin";
        }

        redirectAttributes.addFlashAttribute("response", "권한 수정 완료");
        return "redirect:/automessage/admin";
    }

    @PostMapping("/automessage/admin/{id}")
    public String deleteMember(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            membersService.adminDelete(id);
            redirectAttributes.addFlashAttribute("response", "삭제 완료");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("response", "삭제 실패");
        }
        return "redirect:/automessage/admin";
    }
}
