package com.example.usedlion.controller;

import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.repository.UserInformationRepository;
import com.example.usedlion.security.CustomUserDetails;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Controller
public class ProfileController {

    private final UserInformationRepository userRepo;

    public ProfileController(UserInformationRepository userRepo) {
        this.userRepo = userRepo;
    }

    private boolean isProfileComplete(UserInformation user) {
        UserInformation freshUser = userRepo.findByEmail(user.getEmail());
        return freshUser != null && freshUser.isProfileComplete();
    }

    @GetMapping("/complete-profile")
    public String showProfileForm(HttpSession session, Model model) {
        UserInformation pendingUser = (UserInformation) session.getAttribute("pendingUser");
        if (pendingUser == null) {
            return "redirect:/dashboard";
        }

        UserInformation freshUser = userRepo.findByEmail(pendingUser.getEmail());
        if (freshUser != null && freshUser.isProfileComplete()) {
            return "redirect:/dashboard";
        }

        model.addAttribute("user", pendingUser);
        return "complete-profile";
    }

    @PostMapping("/complete-profile")
    public String submitProfile(@ModelAttribute UserInformation updatedUser, HttpSession session) {
        UserInformation pendingUser = (UserInformation) session.getAttribute("pendingUser");
        if (pendingUser == null) {
            return "redirect:/";
        }

        pendingUser.setNickname(updatedUser.getNickname());
        pendingUser.setRegion(updatedUser.getRegion());

        // ✅ Only mark profile complete if required fields are filled
        if (pendingUser.hasRequiredProfileFields()) {
            pendingUser.setProfileComplete(true);
        }

        userRepo.updateProfileFields(pendingUser); // implement this in the repository

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
            pendingUser, auth.getCredentials(), auth.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        session.removeAttribute("pendingUser");

        return "redirect:/dashboard?signupSuccess=true";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@RequestParam String nickname,
                                @RequestParam String region,
                                Authentication auth,
                                Model model) {
        String email = null;

        if (auth.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            email = customUserDetails.getUsername();
        } else if (auth.getPrincipal() instanceof OAuth2User oauth) {
            email = oauth.getAttribute("email");
        }

        if (email == null) {
            model.addAttribute("error", "사용자 인증 정보를 불러오지 못했습니다.");
            return "error";  // you can also render a generic error page
        }

        UserInformation user = userRepo.findByEmail(email);
        if (user == null) {
            model.addAttribute("error", "사용자 정보를 찾을 수 없습니다.");
            return "error";
        }

        user.setNickname(nickname);
        user.setRegion(region);
        userRepo.updateProfileFields(user);

        return "redirect:/profile?success=true";
    }
}
