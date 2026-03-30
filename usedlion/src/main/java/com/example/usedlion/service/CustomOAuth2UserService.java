package com.example.usedlion.service;

import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.repository.UserInformationRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserInformationRepository userRepo;

    public CustomOAuth2UserService(UserInformationRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User oauthUser = super.loadUser(req);
        String email = oauthUser.getAttribute("email"); // 이메일 받기
        String localPart = email.substring(0, email.indexOf("@")); // 이메일에서 @ 이전의 값을 localPart에 저장

        UserInformation user = userRepo.findByEmail(email);
        if (user == null) {
            user = new UserInformation();
            user.setEmail(email);
            user.setPassword(null); // 비밀번호는 Null로
            user.setUsername(localPart); // username @이전의 값으로 저장
            user.setNickname(null); // nickname @이전의 값으로 저장
            user.setProvider("google");
            user.setProviderId(oauthUser.getName());
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now());
            userRepo.save(user);
        }

        else if (!"google".equals(user.getProvider())) { // 기존 사용자 검사 -> provicder가 local이면
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_provider"),
                    "이미 일반 계정으로 가입된 이메일입니다. 이메일/비밀번호로 로그인하세요.");
        }
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                oauthUser.getAttributes(),
                "email");
    }
}
