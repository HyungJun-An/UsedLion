package com.example.usedlion.dto;

import com.example.usedlion.entity.Chat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatResponseDTO {
    private int id;
    private int userId;
    private String content;
    private String userName;
    private LocalDateTime createdAt ;

    public ChatResponseDTO(Chat chat /*User user*/, String nickname) {
        this.id = chat.getMsgId();
        this.userId = chat.getUserId();
        this.content = chat.getContent();
//        this.userName = User.getUserNickName();
        this.userName = nickname;
        this.createdAt = chat.getCreatedAt();
    }

}
