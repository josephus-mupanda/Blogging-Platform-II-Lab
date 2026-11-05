
package com.example.Blogging_Platform_II.config;

import com.example.Blogging_Platform_II.api.ApiEnvelope;
import com.example.Blogging_Platform_II.model.Blog.Account;
import com.example.Blogging_Platform_II.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectMapper json) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes.requestMatchers("/actuator/metrics/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setHeader("WWW-Authenticate", "Basic realm=\"Blogging Platform\"");
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    json.writeValue(response.getOutputStream(), ApiEnvelope.error("Invalid credentials.", null));
                }))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    json.writeValue(response.getOutputStream(), ApiEnvelope.error("Administrator access is required.", null));
                }))
                .build();
    }

    @Bean
    UserDetailsService accountDetails(UserRepository users) {
        return username -> {
            Account account = users.account(username.strip())
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials."));
            return new User(account.user().username(), account.passwordHash(), account.user().active(),
                    true, true, true, AuthorityUtils.createAuthorityList("ROLE_" + account.user().role()));
        };
    }

    @Bean
    PasswordEncoder passwords() {
        var current = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        return new PasswordEncoder() {
            private static final String PREFIX = "{pbkdf2}";

            @Override
            public String encode(CharSequence password) {
                return PREFIX + current.encode(password);
            }

            @Override
            public boolean matches(CharSequence password, String encoded) {
                if (encoded.startsWith(PREFIX)) {
                    return current.matches(password, encoded.substring(PREFIX.length()));
                }
                if (!encoded.matches("[a-f0-9]{64}")) {
                    return false;
                }
                try {
                    byte[] digest = MessageDigest.getInstance("SHA-256")
                            .digest(password.toString().getBytes(StandardCharsets.UTF_8));
                    return MessageDigest.isEqual(digest, HexFormat.of().parseHex(encoded));
                } catch (NoSuchAlgorithmException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        };
    }
}
