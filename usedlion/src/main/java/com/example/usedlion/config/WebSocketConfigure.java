package com.example.usedlion.config;

import com.example.usedlion.service.ChatServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfigure implements WebSocketConfigurer {
    private final ChatServiceImpl chatService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatService, "/ws/chat/{post_id}") // 게시판 채팅
                .setAllowedOriginPatterns("*");
        registry.addHandler(chatService, "/ws/private/{userA}/{userB}") // 1대1 채팅
                .setAllowedOriginPatterns("*");
    }
}