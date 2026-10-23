package com.hakkushon.calendarapp.controller;

import org.springframework.mail.MailException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.domain.User;
import com.hakkushon.calendarapp.service.OtpService;
import com.hakkushon.calendarapp.service.OtpVerifyResult;
import com.hakkushon.calendarapp.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * ユーザー設定(ニックネームの変更、2段階認証のオン・オフ)。
 *
 * ※ URLは /settings 以下。SecurityConfigでは /auth/** だけがログイン不要なので、
 *    /settings/** は自動的に「ログイン必須」になる(SecurityConfigの変更は不要)。
 *
 * 2段階認証をオンにするときは、いきなり有効にせず、まず確認コードをメールで送って、
 * 入力できたことを確かめてから有効にする(メールが届かない状態で有効にして、
 * 次回からログインできなくなるのを防ぐため)。
 * オフにするときは、離席中に他人にやめられないよう、パスワードの入力を求める。
 */
@Controller
public class SettingsController {

    private static final int NAME_MAX_LENGTH = 50;

    private final UserService userService;
    private final OtpService otpService;

    public SettingsController(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    @GetMapping("/settings")
    public String index(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        User user = userService.findUser(loginUser.getId());
        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("twoFaEnabled", user.isIs2faEnabled());
        return "settings/index";
    }

    // ---- ニックネーム ----

    @PostMapping("/settings/name")
    public String updateName(@RequestParam(required = false) String name,
                             @AuthenticationPrincipal LoginUser loginUser,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.isEmpty()) {
            redirectAttributes.addFlashAttribute("nameError", "ニックネームを入力してください");
        } else if (trimmed.codePointCount(0, trimmed.length()) > NAME_MAX_LENGTH) {
            redirectAttributes.addFlashAttribute("nameError", NAME_MAX_LENGTH + "文字以内で入力してください");
        } else {
            userService.updateName(loginUser.getId(), trimmed);
            refreshAuthentication(loginUser.getId(), request);
            redirectAttributes.addFlashAttribute("message", "ニックネームを変更しました");
        }
        return "redirect:/settings";
    }

    // ---- 2段階認証をオンにする(メールでコードを確認してから有効にする) ----

    @PostMapping("/settings/2fa/start")
    public String startTwoFa(@AuthenticationPrincipal LoginUser loginUser, RedirectAttributes redirectAttributes) {
        User user = userService.findUser(loginUser.getId());
        if (user.isIs2faEnabled()) {
            return "redirect:/settings";
        }
        if (!sendCode(user, redirectAttributes)) {
            return "redirect:/settings";
        }
        return "redirect:/settings/2fa/verify";
    }

    @GetMapping("/settings/2fa/verify")
    public String verifyForm(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        User user = userService.findUser(loginUser.getId());
        if (user.isIs2faEnabled()) {
            return "redirect:/settings";
        }
        model.addAttribute("userEmail", user.getEmail());
        return "settings/two-factor-verify";
    }

    @PostMapping("/settings/2fa/verify")
    public String verify(@RequestParam String code,
                         @AuthenticationPrincipal LoginUser loginUser,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        OtpVerifyResult result = otpService.verify(loginUser.getId(), code.strip());
        if (result == OtpVerifyResult.SUCCESS) {
            userService.enableTwoFactor(loginUser.getId());
            refreshAuthentication(loginUser.getId(), request);
            redirectAttributes.addFlashAttribute("message", "2段階認証を有効にしました。次回のログインから認証コードが必要になります");
            return "redirect:/settings";
        }

        String error = switch (result) {
            case EXPIRED -> "認証コードの有効期限が切れています。再送信してください";
            case LOCKED -> "誤入力の回数が上限に達しました。コードを再送信してください";
            case NOT_FOUND -> "認証コードが見つかりません。再送信してください";
            default -> "認証コードが正しくありません";
        };
        model.addAttribute("error", error);
        model.addAttribute("userEmail", userService.findUser(loginUser.getId()).getEmail());
        return "settings/two-factor-verify";
    }

    @PostMapping("/settings/2fa/resend")
    public String resend(@AuthenticationPrincipal LoginUser loginUser, RedirectAttributes redirectAttributes) {
        User user = userService.findUser(loginUser.getId());
        if (user.isIs2faEnabled()) {
            return "redirect:/settings";
        }
        sendCode(user, redirectAttributes);
        return "redirect:/settings/2fa/verify";
    }

    // ---- 2段階認証をオフにする(パスワードで本人確認) ----

    @PostMapping("/settings/2fa/disable")
    public String disable(@RequestParam(required = false) String password,
                          @AuthenticationPrincipal LoginUser loginUser,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        if (!userService.matchesPassword(loginUser.getId(), password)) {
            redirectAttributes.addFlashAttribute("error", "パスワードが違います。2段階認証はそのままです");
            return "redirect:/settings";
        }
        userService.disableTwoFactor(loginUser.getId());
        refreshAuthentication(loginUser.getId(), request);
        redirectAttributes.addFlashAttribute("message", "2段階認証を無効にしました");
        return "redirect:/settings";
    }

    // ---- 内部処理 ----

    /**
     * 確認コードを送る。メールの送信に失敗したら、画面に出すエラーを入れて false を返す。
     * 直前に送ったコードがまだ新しい場合(連打防止)は、送らずに案内だけ出して true を返す。
     */
    private boolean sendCode(User user, RedirectAttributes redirectAttributes) {
        try {
            boolean sent = otpService.issueAndSend(user.getId(), user.getEmail());
            redirectAttributes.addFlashAttribute("message", sent
                    ? "認証コードを送信しました。メールを確認してください"
                    : "直前に送ったコードがまだ使えます。届いていない場合は、少し待ってから再送信してください");
            return true;
        } catch (MailException e) {
            redirectAttributes.addFlashAttribute("error", "メールを送信できませんでした。時間をおいてもう一度お試しください");
            return false;
        }
    }

    /**
     * ログイン中の情報(LoginUserの名前・2段階認証の状態)を、DBの最新の内容に入れ替える。
     * 入れ替えないと、このセッションの間だけ古い名前のままになる。
     */
    private void refreshAuthentication(Long userId, HttpServletRequest request) {
        LoginUser loginUser = userService.loadUserById(userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
