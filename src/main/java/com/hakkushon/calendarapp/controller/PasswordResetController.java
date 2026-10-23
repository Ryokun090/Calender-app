package com.hakkushon.calendarapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hakkushon.calendarapp.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * パスワードを忘れたとき用の画面。URLは /auth/** なので、SecurityConfigの変更なしでログイン前でも開ける。
 */
@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/auth/password/forgot")
    public String forgotForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/auth/password/forgot")
    public String forgot(@RequestParam(required = false) String email, HttpServletRequest request, Model model) {
        // メールのリンクは、いまアクセスしているURL(ホスト名・ポート)をもとに作る
        String base = ServletUriComponentsBuilder.fromContextPath(request)
                .path("/auth/password/reset").build().toUriString();
        passwordResetService.requestReset(email, base);
        // 登録の有無に関わらず、必ず同じ表示にする
        model.addAttribute("sent", true);
        return "auth/forgot-password";
    }

    @GetMapping("/auth/password/reset")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        if (!passwordResetService.isTokenValid(token)) {
            model.addAttribute("invalid", true);
            return "auth/reset-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/auth/password/reset")
    public String reset(@RequestParam(required = false) String token,
                         @RequestParam(required = false) String password,
                         @RequestParam(required = false) String passwordConfirm,
                         Model model) {
        String problem = passwordResetService.resetPassword(token, password, passwordConfirm);
        if (problem == null) {
            return "redirect:/auth/login?reset";
        }
        if (passwordResetService.isTokenValid(token)) {
            model.addAttribute("token", token);   // 入力ミスなので、同じ画面でやり直せる
            model.addAttribute("problem", problem);
        } else {
            model.addAttribute("invalid", true);
        }
        return "auth/reset-password";
    }
}
