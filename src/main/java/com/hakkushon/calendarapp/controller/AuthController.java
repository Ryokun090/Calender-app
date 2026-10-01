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
import com.hakkushon.calendarapp.service.OtpVerifyResult;
import com.hakkushon.calendarapp.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private static final String SIGNUP_USER_ID = "SIGNUP_USER_ID";

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
    public String signup(@Valid @ModelAttribute SignupForm signupForm, BindingResult bindingResult,
                          HttpServletRequest request) {
        if (!signupForm.getPassword().equals(signupForm.getPasswordConfirm())) {
            bindingResult.addError(new FieldError("signupForm", "passwordConfirm", "パスワードが一致しません"));
        }
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }
        Long userId;
        try {
            userId = userService.signup(signupForm);
        } catch (IllegalStateException e) {
            bindingResult.addError(new FieldError("signupForm", "email", e.getMessage()));
            return "auth/signup";
        }

        // 登録直後、まだログインさせずに「2段階認証を設定するか」の画面を挟む
        request.getSession().setAttribute(SIGNUP_USER_ID, userId);
        return "redirect:/auth/signup/2fa-prompt";
    }

    // ---- 新規登録直後の2段階認証プロンプト ----

    @GetMapping("/auth/signup/2fa-prompt")
    public String twoFaPromptForm(HttpServletRequest request) {
        if (getSignupUserId(request) == null) {
            return "redirect:/auth/login";
        }
        return "auth/signup-2fa-prompt";
    }

    @PostMapping("/auth/signup/2fa-prompt/enable")
    public String enableTwoFa(HttpServletRequest request) {
        Long userId = getSignupUserId(request);
        if (userId == null) {
            return "redirect:/auth/login";
        }
        userService.enableTwoFactor(userId);
        request.getSession().removeAttribute(SIGNUP_USER_ID);
        return "redirect:/auth/login?registered";
    }

    @PostMapping("/auth/signup/2fa-prompt/skip")
    public String skipTwoFa(HttpServletRequest request) {
        Long userId = getSignupUserId(request);
        if (userId == null) {
            return "redirect:/auth/login";
        }
        request.getSession().removeAttribute(SIGNUP_USER_ID);
        return "redirect:/auth/login?registered";
    }

    private Long getSignupUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (Long) session.getAttribute(SIGNUP_USER_ID);
    }

    // ---- ここから2段階認証(ログイン時) ----

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

        OtpVerifyResult result = otpService.verify(userId, code);

        switch (result) {
            case SUCCESS -> {
                establishAuthentication(userId, request);
                // TODO: Bの実装後 "/calendars" に変更
                return "redirect:/health";
            }
            case EXPIRED -> model.addAttribute("error", "認証コードの有効期限が切れています。再送信してください");
            case LOCKED -> model.addAttribute("error", "誤入力の回数が上限に達しました。コードを再送信してください");
            case NOT_FOUND -> model.addAttribute("error", "認証コードが見つかりません。再送信してください");
            case INVALID_CODE -> model.addAttribute("error", "認証コードが正しくありません");
        }
        return "auth/verify";
    }

    @PostMapping("/auth/verify/resend")
    public String resend(HttpServletRequest request, Model model) {
        Long userId = getPendingUserId(request);
        if (userId == null) {
            return "redirect:/auth/login";
        }
        LoginUser loginUser = userService.loadUserById(userId);
        boolean sent = otpService.issueAndSend(loginUser.getId(), loginUser.getEmailAddress());
        if (sent) {
            model.addAttribute("resent", true);
        } else {
            model.addAttribute("error", "再送信は少し間隔を空けてから行ってください");
        }
        return "auth/verify";
    }

    private void establishAuthentication(Long userId, HttpServletRequest request) {
        LoginUser loginUser = userService.loadUserById(userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.removeAttribute(TwoFactorAuthenticationSuccessHandler.PENDING_2FA_USER_ID);
    }

    private Long getPendingUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (Long) session.getAttribute(TwoFactorAuthenticationSuccessHandler.PENDING_2FA_USER_ID);
    }
}
