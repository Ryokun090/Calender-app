package com.hakkushon.calendarapp.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.domain.User;
import com.hakkushon.calendarapp.mapper.PasswordResetTokenMapper;
import com.hakkushon.calendarapp.mapper.UserMapper;

/**
 * パスワード再設定。
 *   1. メールアドレスを入れる → 登録されていれば、30分有効の再設定リンクをメールで送る
 *   2. リンクを開く → 新しいパスワードを入れる → 変更して、リンクは使用済みにする
 * 「そのメールアドレスが登録されているか」が外から分からないよう、1.の画面の表示はどちらの場合も同じにする。
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72; // BCryptが扱えるのは72バイトまで
    private static final int TOKEN_BYTES = 32;        // 16進にすると64文字
    private static final int EXPIRY_MINUTES = 30;
    private static final int RESEND_INTERVAL_SECONDS = 60;

    private final UserMapper userMapper;
    private final PasswordResetTokenMapper tokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserMapper userMapper, PasswordResetTokenMapper tokenMapper,
                                 PasswordEncoder passwordEncoder, JavaMailSender mailSender,
                                 @Value("${spring.mail.username:}") String fromAddress) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * 再設定メールを送る。メールアドレスが未登録・直前に送ったばかり・メール送信に失敗、のどの場合も
     * 呼び出し元には何も伝えない(画面の表示を同じにするため)。失敗はログに残す。
     *
     * @param resetUrlBase 例: https://example.com/auth/password/reset  (末尾に ?token=... を付けて使う)
     */
    @Transactional
    public void requestReset(String email, String resetUrlBase) {
        if (email == null || email.isBlank()) {
            return;
        }
        Optional<User> found = userMapper.findByEmail(email.trim());
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();

        if (tokenMapper.countIssuedSince(user.getId(), LocalDateTime.now().minusSeconds(RESEND_INTERVAL_SECONDS)) > 0) {
            return; // 直前に送ったばかり
        }

        tokenMapper.invalidateAllForUser(user.getId());
        String rawToken = generateToken();
        tokenMapper.insert(user.getId(), sha256(rawToken), LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));

        SimpleMailMessage message = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(user.getEmail());
        message.setSubject("【SukiMatch】パスワード再設定のご案内");
        message.setText(user.getName() + " さん\n\n"
                + "パスワードの再設定を受け付けました。次のリンクを開いて、新しいパスワードを設定してください。\n"
                + "(リンクの有効期限は " + EXPIRY_MINUTES + " 分です)\n\n"
                + resetUrlBase + "?token=" + rawToken + "\n\n"
                + "心当たりがない場合は、このメールは無視してください。パスワードは変更されません。\n");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // 画面には出さない(登録の有無が分かってしまうため)。原因はログで確認する
            log.error("パスワード再設定メールの送信に失敗しました: userId={}", user.getId(), e);
        }
    }

    /** リンクのトークンがまだ使えるか(再設定画面を出してよいか)。 */
    public boolean isTokenValid(String rawToken) {
        if (!looksLikeToken(rawToken)) {
            return false;
        }
        return tokenMapper.findValidUserId(sha256(rawToken)) != null;
    }

    /**
     * パスワードを変更する。
     *
     * @return null なら成功。失敗のときは画面に出せるメッセージ
     */
    @Transactional
    public String resetPassword(String rawToken, String newPassword, String confirm) {
        String problem = checkPassword(newPassword, confirm);
        if (problem != null) {
            return problem;
        }
        if (!looksLikeToken(rawToken)) {
            return "リンクが無効か、有効期限が切れています。もう一度やり直してください";
        }
        String hash = sha256(rawToken);
        Long userId = tokenMapper.findValidUserId(hash);
        if (userId == null || tokenMapper.markUsed(hash) != 1) {
            return "リンクが無効か、有効期限が切れています。もう一度やり直してください";
        }
        userMapper.updatePasswordHash(userId, passwordEncoder.encode(newPassword));
        tokenMapper.invalidateAllForUser(userId);
        return null;
    }

    private String checkPassword(String password, String confirm) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return "パスワードは" + MIN_PASSWORD_LENGTH + "文字以上で入力してください";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return "パスワードが長すぎます(半角英数字で72文字まで)";
        }
        if (!password.equals(confirm)) {
            return "パスワードが一致しません";
        }
        return null;
    }

    private boolean looksLikeToken(String token) {
        return token != null && token.matches("[0-9a-f]{" + (TOKEN_BYTES * 2) + "}");
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
