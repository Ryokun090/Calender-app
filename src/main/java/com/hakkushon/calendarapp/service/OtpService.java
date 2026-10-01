package com.hakkushon.calendarapp.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.domain.OtpCode;
import com.hakkushon.calendarapp.mapper.OtpMapper;

@Service
public class OtpService {

    private static final int EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 30;

    private final OtpMapper otpMapper;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpMapper otpMapper, JavaMailSender mailSender) {
        this.otpMapper = otpMapper;
        this.mailSender = mailSender;
    }

    /**
     * 6桁のコードを発行してDBに保存し、メールで送信する。
     * 直前のコードがまだ新しい(クールダウン中)場合は送信せず false を返す。
     */
    @Transactional
    public boolean issueAndSend(Long userId, String email) {
        Optional<OtpCode> latest = otpMapper.findLatestUnusedByUserId(userId);
        if (latest.isPresent()) {
            long secondsSinceLast = Duration.between(latest.get().getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < RESEND_COOLDOWN_SECONDS) {
                return false; // 連打防止
            }
        }

        String code = String.format("%06d", random.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setUserId(userId);
        otp.setCode(code);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        otpMapper.insert(otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("【共有カレンダーアプリ】ログイン認証コード");
        message.setText("以下の認証コードを" + EXPIRY_MINUTES + "分以内に入力してください。\n\n認証コード: " + code);
        mailSender.send(message);
        return true;
    }

    /**
     * 入力されたコードを検証する。
     * 誤入力が続くとロックされ、新しいコードの再送信が必要になる。
     */
    @Transactional
    public OtpVerifyResult verify(Long userId, String inputCode) {
        Optional<OtpCode> latest = otpMapper.findLatestUnusedByUserId(userId);
        if (latest.isEmpty()) {
            return OtpVerifyResult.NOT_FOUND;
        }

        OtpCode otp = latest.get();

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            return OtpVerifyResult.EXPIRED;
        }

        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            otpMapper.markUsed(otp.getId()); // これ以上は使えないようにする
            return OtpVerifyResult.LOCKED;
        }

        if (!otp.getCode().equals(inputCode)) {
            otpMapper.incrementAttempt(otp.getId());
            return OtpVerifyResult.INVALID_CODE;
        }

        otpMapper.markUsed(otp.getId());
        return OtpVerifyResult.SUCCESS;
    }
}
