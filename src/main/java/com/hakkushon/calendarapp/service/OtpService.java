package com.hakkushon.calendarapp.service;

import java.security.SecureRandom;
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

    private final OtpMapper otpMapper;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpMapper otpMapper, JavaMailSender mailSender) {
        this.otpMapper = otpMapper;
        this.mailSender = mailSender;
    }

    /**
     * 6桁のコードを発行してDBに保存し、メールで送信する。
     */
    @Transactional
    public void issueAndSend(Long userId, String email) {
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
    }

    /**
     * 入力されたコードを検証する。有効なら使用済みにして true を返す。
     * 試行回数の上限は今回は入れていない(必要になったらここに追加)。
     */
    @Transactional
    public boolean verify(Long userId, String inputCode) {
        Optional<OtpCode> latest = otpMapper.findLatestUnusedByUserId(userId);
        if (latest.isEmpty()) {
            return false;
        }
        OtpCode otp = latest.get();
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (!otp.getCode().equals(inputCode)) {
            return false;
        }
        otpMapper.markUsed(otp.getId());
        return true;
    }
}
