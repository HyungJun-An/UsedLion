package com.example.usedlion.security;

import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.repository.UserInformationRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserInformationRepository userRepo;

    public CustomOAuth2SuccessHandler(UserInformationRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();

        String email = oauthUser.getAttribute("email");

        UserInformation user = userRepo.findByEmail(email);

        if (user == null) {
            user = new UserInformation();
            user.setEmail(email);
            user.setUsername(oauthUser.getAttribute("name")); // or derive from email
            user.setNickname(null);
            user.setProvider("google");
            user.setProviderId(oauthUser.getAttribute("sub"));
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now());
            user.setProfileComplete(false);
            userRepo.save(user);
        }

        request.getSession().setAttribute("user", user);

        if (!user.isProfileComplete()) {
            request.getSession().setAttribute("pendingUser", user);
            response.sendRedirect("/complete-profile");
        } else {
            // ✅ Ensure session contains user info just like local login
            request.getSession().setAttribute("user", user);

            boolean isNew = request.getSession().getAttribute("newGoogleSignup") != null;
            if (isNew) {
                request.getSession().removeAttribute("newGoogleSignup");
                response.sendRedirect("/dashboard?signupSuccess=true");
            } else {
                response.sendRedirect("/dashboard");
            }
        }
    }
}