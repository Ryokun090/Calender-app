package com.hakkushon.calendarapp.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.common.TwoFactorAuthenticationSuccessHandler;
import com.hakkushon.calendarapp.dto.SignupForm;
import com.hakkushon.calendarapp.service.OtpService;
import com.hakkushon.calendarapp.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UserService userService;
    private final OtpService otpService;

    public AuthController(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    // ログインのPOST自体はSpring Securityのformログイン機構が処理するので、
    // ここではGETでログイン画面を表示するだけでよい。
    @GetMapping("/auth/login")
    public String loginForm() {
        return "auth/login";
    }

    @GetMapping("/auth/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "auth/signup";
    }

    @PostMapping("/auth/signup")
    public String signup(@Valid @ModelAttribute SignupForm signupForm, BindingResult bindingResult) {
        if (!signupForm.getPassword().equals(signupForm.getPasswordConfirm())) {
            bindingResult.addError(new FieldError("signupForm", "passwordConfirm", "パスワードが一致しません"));
        }
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }
        try {
            userService.signup(signupForm);
        } catch (IllegalStateException e) {
            bindingResult.addError(new FieldError("signupForm", "email", e.getMessage()));
            return "auth/signup";
        }
        return "redirect:/auth/login?registered";
    }

    // ---- ここから2段階認証 ----

    @GetMapping("/auth/verify")
    public String verifyForm(HttpServletRequest request) {
        if (getPendingUserId(request) == null) {
            return "redirect:/auth/login";
        }
        return "auth/verify";
    }

    @PostMapping("/auth/verify")
    public String verify(@RequestParam String code, HttpServletRequest request, Model model) {
        Long userId = getPendingUserId(request);
        if (userId == null) {
            return "redirect:/auth/login";
        }

        if (!otpService.verify(userId, code)) {
            model.addAttribute("error", "認証コードが正しくないか、有効期限が切れています");
            return "auth/verify";
        }

        // ここで初めて本当のログイン状態を確立する
        LoginUser loginUser = userService.loadUserById(userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.removeAttribute(TwoFactorAuthenticationSuccessHandler.PENDING_2FA_USER_ID);

        // TODO: Bの実装後 "/calendars" に変更
        return "redirect:/health";
    }

    @PostMapping("/auth/verify/resend")
    public String resend(HttpServletRequest request, Model model) {
        Long userId = getPendingUserId(request);
        if (userId == null) {
            return "redirect:/auth/login";
        }
        LoginUser loginUser = userService.loadUserById(userId);
        otpService.issueAndSend(loginUser.getId(), loginUser.getEmailAddress());
        model.addAttribute("resent", true);
        return "auth/verify";
    }

    private Long getPendingUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (Long) session.getAttribute(TwoFactorAuthenticationSuccessHandler.PENDING_2FA_USER_ID);
    }
}
