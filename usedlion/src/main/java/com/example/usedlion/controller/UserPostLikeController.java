package com.example.usedlion.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.usedlion.service.UserPostLikeService;
import com.example.usedlion.service.UserService;

@Controller
@RequestMapping
@CrossOrigin
public class UserPostLikeController {
    private final UserPostLikeService userPostLikeService;
    private final UserService userService;

    public UserPostLikeController(UserPostLikeService userPostLikeService, UserService userService) {
        this.userService = userService;
        this.userPostLikeService = userPostLikeService;
    }

    @PostMapping("/post/like/{postId}")
    public String likePost(@PathVariable Integer postId, Model model, Principal principal) {
        Integer userId = userService.getUserByEmail(principal.getName()).getUserId();
        userPostLikeService.likePost(postId, userId);
        // Logic to like a post
        return "redirect:/post/" + postId;
    }

    @PostMapping("/post/unlike/{postId}")
    public String unlikePost(@PathVariable Integer postId, Principal principal) {
        Integer userId = userService.getUserByEmail(principal.getName()).getUserId();
        userPostLikeService.unlikePost(postId, userId);
        // Logic to unlike a post
        return "redirect:/post/" + postId;
    }
}
