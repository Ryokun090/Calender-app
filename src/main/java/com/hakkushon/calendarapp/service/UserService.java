package com.hakkushon.calendarapp.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.domain.User;
import com.hakkushon.calendarapp.dto.SignupForm;
import com.hakkushon.calendarapp.mapper.UserMapper;

@Service
public class UserService implements UserDetailsService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spring Securityがログイン時に呼ぶ。共通のLoginUserを返すことで、
     * 全ドメインが同じ書き方(@AuthenticationPrincipal LoginUser)で
     * ログイン中ユーザーを受け取れるようにしている。
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("メールアドレスまたはパスワードが違います"));
        return toLoginUser(user);
    }

    /**
     * 2段階認証のコード確認が終わったあと、userIdから改めてLoginUserを組み立てるために使う。
     */
    public LoginUser loadUserById(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません"));
        return toLoginUser(user);
    }

    private LoginUser toLoginUser(User user) {
        return new LoginUser(user.getId(), user.getName(), user.getEmail(), user.getPasswordHash(), user.isIs2faEnabled());
    }

    /**
     * 新規登録。
     * @throws IllegalStateException すでに同じメールアドレスが登録済みの場合
     */
    @Transactional
    public void signup(SignupForm form) {
        if (userMapper.findByEmail(form.getEmail()).isPresent()) {
            throw new IllegalStateException("このメールアドレスはすでに登録されています");
        }
        User user = new User();
        user.setName(form.getName());
        user.setEmail(form.getEmail());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        userMapper.insert(user);
    }
}
