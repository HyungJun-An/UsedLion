package com.example.usedlion.controller;

import com.example.usedlion.security.CustomUserDetails;
import com.example.usedlion.dto.Post;
import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.repository.UserInformationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.usedlion.dto.PostDetailDto;
import com.example.usedlion.dto.PostImage;
import com.example.usedlion.security.CustomUserDetails;
import com.example.usedlion.service.PostService;

import jakarta.servlet.http.HttpSession;

@Controller // @RestController와는 별도!
public class PageController {
    private final PostService postService;
    private final UserInformationRepository userRepo;

    public PageController(PostService postService, UserInformationRepository userRepo) {
        this.userRepo = userRepo;
        this.postService = postService;
    }

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("hideHeader", "true");
        model.addAttribute("hideFooter", true);
        return "landing"; // 로그인 화면
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal OAuth2User oauth2User,
            Authentication authentication,
            Model model) {
        String name = "Guest";
        if (oidcUser != null) {
            name = oidcUser.getFullName();
        } else if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails cud = (CustomUserDetails) authentication.getPrincipal();
            name = cud.getUser().getNickname();
        } else if (authentication != null
                && authentication.getPrincipal() instanceof UserDetails) {
            name = ((UserDetails) authentication.getPrincipal()).getUsername(); // 폼 로그인 사용자명 가져오기
        }
        model.addAttribute("userName", name);

        // Example post data — replace with real DB call

        List<PostDetailDto> posts = postService.getAllPostDetail();
        List<PostImage> postImages = postService.makePostImage(posts);
        model.addAttribute("posts", postImages);

        return "dashboard";
    }

    @GetMapping("/profile")
    public String profilePage(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            CustomUserDetails cud = (CustomUserDetails) principal;
            model.addAttribute("user", cud.getUser());
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            // you must fetch the DB user by email
            String email = oauthUser.getAttribute("email");
            UserInformation user = userRepo.findByEmail(email);
            if (user == null)
                return "redirect:/"; // fail-safe
            model.addAttribute("user", user);
        } else {
            return "redirect:/";
        }

        return "profile";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/chat")
    public String chat(
            @AuthenticationPrincipal OAuth2User oauth2User,
            Authentication authentication,
            Model model) {
        String nickname = "Guest";

        // ① OAuth2 로그인 (구글) 사용자의 프로필 이름이 곧 nickname
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails cud = (CustomUserDetails) authentication.getPrincipal();
            nickname = cud.getUser().getNickname();
        } else if (oauth2User != null && oauth2User.getAttribute("name") != null) {
            nickname = oauth2User.getAttribute("name");
        }
        // ② 로컬 로그인 사용자는 UserInformationRepository를 이용해 DB에서 nickname 조회
        else if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails cud = (CustomUserDetails) authentication.getPrincipal();
            nickname = cud.getUser().getNickname(); // getUser() → UserInformation DTO
        }

        model.addAttribute("nickname", nickname);
        return "chat";
    }
}
