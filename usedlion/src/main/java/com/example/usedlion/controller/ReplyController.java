package com.example.usedlion.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.usedlion.dto.ReplyDetailDto;
import com.example.usedlion.entity.Reply;
import com.example.usedlion.entity.UserInformation;
import com.example.usedlion.service.ReplyService;
import com.example.usedlion.service.UserService;

@Controller
@RequestMapping("/reply")
@CrossOrigin
public class ReplyController {
    private final ReplyService replyService;
    private final UserService userService;

    public ReplyController(ReplyService replyService, UserService userService) {
        this.replyService = replyService;
        this.userService = userService;
    }

    @GetMapping("/{postId}")
    public List<ReplyDetailDto> getReplyByPostId(@PathVariable Integer postId) {
        return replyService.getReplyByPostId(postId);
    }

    @PostMapping("/{postId}")
    public String createReply(@PathVariable Integer postId, @RequestParam String content, Principal principal) {

        Reply reply = new Reply();
        reply.setContent(content);
        reply.setPostId(postId);
        UserInformation user = userService.getUserByEmail(principal.getName());
        reply.setUserId(user.getUserId());
        reply.setRef(0);
        reply.setLevel(0);
        reply.setCreated_at(LocalDateTime.now());

        replyService.createReply(reply);
        reply.setRef(0);
        replyService.createReply(reply);

        return "redirect:/post/" + postId;
    }

    @PostMapping("/{postId}/{parentReplyId}")
    public String createReReply(@PathVariable Integer postId, @PathVariable Integer parentReplyId,
            @RequestParam String content, Principal principal) {

        Reply reply = new Reply();
        Reply target = replyService.getReplyByReplyId(parentReplyId);
        reply.setContent(content);
        reply.setPostId(postId);
        UserInformation user = userService.getUserByEmail(principal.getName());
        reply.setUserId(user.getUserId());
        reply.setRef(target.getId());
        reply.setLevel(target.getLevel() + 1);
        reply.setCreated_at(LocalDateTime.now());

        replyService.createReply(reply);
        return "redirect:/post/" + postId;
    }

    // ** 수정 폼 프래그먼트 반환 (GET) */
    @GetMapping("/edit/{replyId}")
    public String showEditForm(
            @PathVariable Integer replyId,
            Model model,
            Principal principal) {
        Reply reply = replyService.getReplyByReplyId(replyId);
        UserInformation user = userService.getUserByEmail(principal.getName());
        if (!reply.getUserId().equals(user.getUserId())) {
            return "redirect:/post/" + reply.getPostId();
        }
        model.addAttribute("reply", reply);
        return "edit :: editForm";
    }

    /** 실제 수정 처리 (POST) */
    @PostMapping("/edit/{replyId}")
    public String updateReply(
            @PathVariable Integer replyId,
            @RequestParam String content,
            Principal principal) {
        Reply reply = replyService.getReplyByReplyId(replyId);
        UserInformation user = userService.getUserByEmail(principal.getName());
        if (!reply.getUserId().equals(user.getUserId())) {
            return "redirect:/post/" + reply.getPostId();
        }
        reply.setContent(content);
        replyService.updateReply(replyId, reply);
        return "redirect:/post/" + reply.getPostId();
    }

    @PostMapping("/delete/{replyId}")
    public String deleteReply(@PathVariable Integer replyId, Principal principal) {
        Reply reply = replyService.getReplyByReplyId(replyId);

        // 권한 검사
        UserInformation user = userService.getUserByEmail(principal.getName());
        if (!reply.getUserId().equals(user.getUserId())) {
            return "redirect:/post/" + reply.getPostId();
        }

        replyService.recursiveDelete(reply);
        return "redirect:/post/" + reply.getPostId();
    }

}
