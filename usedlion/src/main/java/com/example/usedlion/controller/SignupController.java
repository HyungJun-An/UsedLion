package com.example.usedlion.controller;

import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.repository.UserInformationRepository;
import com.example.usedlion.security.CustomUserDetails;
import jakarta.servlet.ServletException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class SignupController {
    private final UserInformationRepository userRepo;
    private final PasswordEncoder passwordEncoder; //비밀번호 BCrypt로 저장
    public SignupController(UserInformationRepository userRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @GetMapping("/signup")
    public String signupForm(org.springframework.ui.Model model) {
        model.addAttribute("user", new UserInformation());
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@ModelAttribute UserInformation user, jakarta.servlet.http.HttpServletRequest request) {
        System.out.println("🟢 SignupController: received signup POST request for " + user.getEmail());
        user.setProvider("local");
        user.setProviderId(null);
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());

        String rawPassword = user.getPassword(); // save before encoding
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepo.save(user);

        try {
            // ✅ Perform login through servlet
            request.login(user.getEmail(), rawPassword); // Must match username & raw password
        } catch (ServletException e) {
            System.err.println("❌ Login failed after signup: " + e.getMessage());
            return "redirect:/?error";
        }


        return "redirect:/dashboard?signupSuccess=true";  // 축하 후 대시보드 페이지로 리디렉션
    }
}