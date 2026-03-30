package com.example.usedlion.config; // ← update to match your actual package

import com.example.usedlion.security.CustomUserDetailsService;
import com.example.usedlion.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private com.example.usedlion.security.CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
//    @Autowired
//    private CustomOAuth2UserService customOAuth2UserService;
//    private final CustomUserDetailsService customUserDetailsService; //for login
//    private final PasswordEncoder passwordEncoder; // for login
//
//    public SecurityConfig(CustomOAuth2UserService oauth2,
//                          CustomUserDetailsService userDetailsService,
//                          PasswordEncoder pwEncoder) {
//        this.customOAuth2UserService = oauth2;
//        this.customUserDetailsService = userDetailsService;
//        this.passwordEncoder = pwEncoder;
//    }
    //비밀번호 암호화용 Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    // 로컬 로그인 전용 Provider
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider daoAuthProvider,
                                           CustomOAuth2UserService oauth2UserService) throws Exception {
        http
                // 개발 중 CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 접근 허용 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login","/signup", "/css/**", "/js/**", "/images/**"
                                ,"/chat", "/static/**", "/ws/**","/topic/**", "/complete-profile").permitAll()
                        //.requestMatchers("/chat").authenticated() // chat 인증된 사용자만 보이게
                        .anyRequest().authenticated()
                )

                //  ②.② 로컬 로그인 활성화 -> DaoAuthenticationProvider를 AuthenticationManager에 등록
                .authenticationProvider(daoAuthProvider)

                // ② 일반 폼 로그인 활성화
                .formLogin(form -> form
                        .loginPage("/")            // landing.html
                        .loginProcessingUrl("/login") // POST action
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )

                // ③ Google OAuth2 로그인
        System.out.println("✅ SecurityFilterChain: filterChain bean is being executed");
        return http.build();
    }

}