package com.example.usedlion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer postId;

    private String roomId;

    private Integer senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt;

    public Chat(Integer postId, String roomId, Integer senderId, String content) {
        this.postId = postId;
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public void setContent(String content) {this.content = content;}

    public int getMsgId() {return id;}

    public int getPostId() {return postId;}

    public int getUserId() {return senderId;}

    public String getRoomId() {return roomId;}

    public String getContent() {return content;}

    public LocalDateTime getCreatedAt() {return createdAt;}
}
