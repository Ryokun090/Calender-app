package com.hakkushon.calendarapp.common;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * ログイン中ユーザーの情報を表すクラス。
 *
 * Controllerでは以下のように受け取る(全ドメイン共通の書き方):
 *
 *   @GetMapping("/calendars/{id}")
 *   public String detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
 *       Long userId = loginUser.getId();
 *       ...
 *   }
 *
 * 各自が個別にセッションやSecurityContextからユーザー情報を取り出すのは禁止。
 * 必ずこのクラス経由で取得すること。
 */
public class LoginUser implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String passwordHash;

    public LoginUser(Long id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmailAddress() {
        return email;
    }

    // ---- UserDetails実装(Spring Securityが要求するメソッド群) ----

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 今回はロール分けをしないので固定の権限を1つだけ返す
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
