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
    private final CalendarService calendarService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, CalendarService calendarService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.calendarService = calendarService;
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
     * 新規登録。成功したら作成したユーザーのidを返す
     * (登録直後に「2段階認証を設定するか」を尋ねる画面で使うため)。
     * あわせて、本人だけが所属する個人用カレンダーを自動で1つ作る。
     *
     * @throws IllegalStateException すでに同じメールアドレスが登録済みの場合
     */
    @Transactional
    public Long signup(SignupForm form) {
        if (userMapper.findByEmail(form.getEmail()).isPresent()) {
            throw new IllegalStateException("このメールアドレスはすでに登録されています");
        }
        User user = new User();
        user.setName(form.getName());
        user.setEmail(form.getEmail());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        userMapper.insert(user);

        calendarService.createPersonalCalendarForUser(user.getId());

        return user.getId();
    }

    /**
     * 新規登録直後の「2段階認証を設定しますか」画面で、
     * 「設定する」が選ばれたときに呼ぶ。
     */
    @Transactional
    public void enableTwoFactor(Long userId) {
        userMapper.updateTwoFaEnabled(userId, true);
    }

    // ---- ここから設定画面用 ----

    /** 設定画面の表示用に、ユーザーの最新の情報(名前・メール・2段階認証の状態)を取る。 */
    public User findUser(Long userId) {
        return userMapper.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません"));
    }

    /** ニックネームを変更する。呼ぶ側で空欄・文字数のチェックを済ませておくこと。 */
    @Transactional
    public void updateName(Long userId, String name) {
        userMapper.updateName(userId, name);
    }

    /** 設定画面から2段階認証をやめる。 */
    @Transactional
    public void disableTwoFactor(Long userId) {
        userMapper.updateTwoFaEnabled(userId, false);
    }

    /** 入力されたパスワードが、本人のパスワードと一致するか(2段階認証をやめるときの本人確認に使う)。 */
    public boolean matchesPassword(Long userId, String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return false;
        }
        User user = findUser(userId);
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
}
