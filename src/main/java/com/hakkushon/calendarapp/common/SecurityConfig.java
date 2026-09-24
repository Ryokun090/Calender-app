package com.hakkushon.calendarapp.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 暫定のSecurity設定。
 *
 * spring-boot-starter-securityを入れると、デフォルトでは全リクエストが
 * ログイン必須になり、コンソールに毎回ランダムなパスワードが表示される。
 * 環境構築の動作確認(/health)がそれで止まらないよう、いったん全許可にしてある。
 *
 * Aが認証機能を実装する際、このクラスを本来のログイン設定
 * (formLogin、認可ルールなど)に置き換えること。
 * PasswordEncoderのBean定義だけはそのまま使ってよい。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable()); // TODO: Aが本実装時にCSRF設定を見直す

        return http.build();
    }
}
