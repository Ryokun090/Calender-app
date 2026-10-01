package com.hakkushon.calendarapp.common;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.hakkushon.calendarapp.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * パスワード認証が成功した直後に呼ばれる。
 *
 * 2段階認証が無効なユーザーはそのままログイン完了。
 * 有効なユーザーは、ここではまだ本ログインにせず、
 * いったんセッションを作り直して「2段階認証待ち」の状態だけを持たせ、
 * /auth/verify に飛ばす。本当の認証確立はAuthController#verifyで行う。
 */
@Component
public class TwoFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String PENDING_2FA_USER_ID = "PENDING_2FA_USER_ID";

    private final OtpService otpService;

    public TwoFactorAuthenticationSuccessHandler(OtpService otpService) {
        this.otpService = otpService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        if (!loginUser.isTwoFaEnabled()) {
            // TODO: Bの実装後 "/calendars" に変更
            response.sendRedirect("/health");
            return;
        }

        otpService.issueAndSend(loginUser.getId(), loginUser.getEmailAddress());

        // まだ本ログインとして扱わない。認証状態を破棄し、
        // 「2段階認証待ち」の印だけを新しいセッションに残す。
        request.getSession().invalidate();
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute(PENDING_2FA_USER_ID, loginUser.getId());

        response.sendRedirect("/auth/verify");
    }
}
