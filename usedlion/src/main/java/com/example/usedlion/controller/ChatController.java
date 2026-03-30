package com.example.usedlion.controller;

import com.example.usedlion.dto.ChatResponseDTO;
import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.entity.Chat;
import com.example.usedlion.repository.ChatRepository;
import com.example.usedlion.security.CustomUserDetails;
import com.example.usedlion.service.ChatServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatRepository chatRepository;
    private final ChatServiceImpl chatService;



    /**
     * 채팅방 입장
     */
    @GetMapping("/chat/{postId}")
    public String getChat(@PathVariable Long postId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        // Local 로그인
        Long userId = 0L;
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            UserInformation user = userDetails.getUser();

            userId = user.getId(); // 또는 getUserId()
            String username = user.getUsername();

            System.out.println("User ID: " + userId);
            System.out.println("Username: " + username);

            model.addAttribute("userId",userId);
            model.addAttribute("userName",username);
        }

        // 구글 로그인 시
        if(userId==0) {
            if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                String email = oauth2User.getAttribute("email");
                String localPart = email.substring(0, email.indexOf("@"));

                System.out.println("구글 유저 이메일: " + email);
                System.out.println("구글 유저명(localPart): " + localPart);

                userId = chatRepository.findUserIdByEmail(email);
                System.out.println("userId: " + userId);

                model.addAttribute("userId",userId);
                model.addAttribute("userName",localPart);
            }
        }


        return "chat";
    }

    /**
     * 이전 메세지 전체 조회
     */
    @GetMapping("/chat/{postId}/history")
    @ResponseBody
    public List<Chat> getChatHistory(@PathVariable Integer postId) {
        return chatService.getChatHistory(postId); // DB에서 메시지 가져옴
    }

    /**
     * 채팅 내역 페이징 조회
     */
    @GetMapping("/chat/{postId}/page")
    public ResponseEntity<Page<ChatResponseDTO>> getChatPage(
            @PathVariable Integer postId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ){
        Page<ChatResponseDTO> chats = chatService.getChatPage(postId, page, size);
        return ResponseEntity.ok(chats);
    }

    /**
     * 1대1 채팅 창
     */
    // 1대1 채팅 창
    //
    @GetMapping("/chat/private/{userA}/{userB}")
    public String getChat(@PathVariable Integer userA,
                          @PathVariable Integer userB,
                          Model model) {
        model.addAttribute("userA", userA);
        model.addAttribute("userB", userB);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        // Local 로그인
        Long userId = 0L;
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            UserInformation user = userDetails.getUser();

            userId = user.getId(); // 또는 getUserId()
            String username = user.getUsername();

            System.out.println("User ID: " + userId);
            System.out.println("Username: " + username);

            model.addAttribute("userId",userId);
            model.addAttribute("userName",username);
        }

        // 구글 로그인 시
        if(userId==0) {
            if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                String email = oauth2User.getAttribute("email");
                String localPart = email.substring(0, email.indexOf("@"));

                System.out.println("구글 유저 이메일: " + email);
                System.out.println("구글 유저명(localPart): " + localPart);

                userId = chatRepository.findUserIdByEmail(email);
                System.out.println("userId: " + userId);

                model.addAttribute("userId",userId);
                model.addAttribute("userName",localPart);
            }
        }
        return "private_chat";
    }

    /**
     * 1대1 채팅 내역 페이징 조회
     */
    @GetMapping("/chat/private/history/{userA}/{userB}")
    public ResponseEntity<Page<ChatResponseDTO>> getPrivateChatPage(
            @PathVariable Integer userA,
            @PathVariable Integer userB,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ){
        String room_id;
        if(userA < userB) {
            room_id = userA + "_" + userB;
        } else {
            room_id = userB + "_" + userA;
        }
        Page<ChatResponseDTO> chats = chatService.getPrivateChatPage(room_id, page, size);
        return ResponseEntity.ok(chats);
    }

}
