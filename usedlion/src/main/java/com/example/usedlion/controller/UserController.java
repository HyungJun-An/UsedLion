package com.example.usedlion.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.usedlion.dto.PostDetailDto;
import com.example.usedlion.dto.PostImage;
import com.example.usedlion.dto.ReplyDetailDto;
import com.example.usedlion.entity.Report;
import com.example.usedlion.entity.UserInformation;
import com.example.usedlion.entity.UserPostLike;
import com.example.usedlion.service.ImageService;
import com.example.usedlion.service.PostService;
import com.example.usedlion.service.ReplyService;
import com.example.usedlion.service.ReportService;
import com.example.usedlion.service.UserPostLikeService;
import com.example.usedlion.service.UserService;

@Controller
@CrossOrigin
public class UserController {
    private final PasswordEncoder passwordEncoder;
    private final PostService postService;
    private final UserService userService;
    private final ReportService reportService;
    private final ReplyService replyService;
    private final ImageService imageService;
    private final UserPostLikeService userPostLikeService;

    public UserController(PostService postService, UserService userService,
            ReportService reportService,
            ReplyService replyService, ImageService imageService, UserPostLikeService userPostLikeService) {
        this.replyService = replyService;
        this.reportService = reportService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.postService = postService;
        this.userService = userService;
        this.imageService = imageService;
        this.userPostLikeService = userPostLikeService;
    }

    @GetMapping("/user/{userId}")
    public String getUser(@PathVariable Integer userId, Model model, Principal principal) {
        UserInformation user = userService.getUserById(userId);

        List<PostDetailDto> posts = postService.searchPosts(null, user.getUsername(), null, null);
        List<PostImage> postImages = postService.makePostImage(posts);
        List<Report> reports = reportService.getByUserId(userId);
        List<ReplyDetailDto> replies = replyService.getReplyByUserId(userId);
        List<UserPostLike> likes = userPostLikeService.getUserPostLikeByUserId(userId);

        List<PostDetailDto> likePosts = new ArrayList<>();

        for (UserPostLike like : likes) {
            PostDetailDto post = postService.getPostDetailByPostId(like.getPostId());
            likePosts.add(post);
        }
        List<PostImage> likePostImages = postService.makePostImage(likePosts);

        model.addAttribute("user", user);
        model.addAttribute("posts", postImages);
        model.addAttribute("postCount", posts.size());
        model.addAttribute("reportCount", reports.size());
        model.addAttribute("reports", reports);
        model.addAttribute("replies", replies);
        model.addAttribute("replyCount", replies.size());
        model.addAttribute("likes", likePostImages);
        model.addAttribute("likeCount", likePosts.size()); // Updated to use likePosts.size()
        return "userDetail";
    }

    @PostMapping("/report/{postId}/{userId}")
    public String reportPost(@PathVariable Integer postId, @PathVariable Integer userId,
            @RequestParam("reason") String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String username = principal.getName();
        UserInformation user = userService.getUserByEmail(username);
        Report report = new Report();
        report.setReporterId(user.getUserId());
        report.setTargetId(userId);
        report.setContent(reason);

        reportService.createReport(report);
        return "redirect:/post/" + postId; // Redirect to the post detail page after reporting
    }

}
