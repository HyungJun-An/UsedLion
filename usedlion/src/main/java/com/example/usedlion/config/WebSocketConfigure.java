package com.example.usedlion.config;

import com.example.usedlion.service.ChatServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfigure implements WebSocketConfigurer {
    private final ChatServiceImpl chatService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatService, "/ws/chat/{post_id}")
                .setAllowedOriginPatterns("*");
        registry.addHandler(chatService, "/ws/private/{userA}/{userB}")
                .setAllowedOriginPatterns("*");
    }

    /**
     * WebSocket 서버 컨테이너 튜닝
     *
     * - maxTextMessageBufferSize : 512KB — 기본 8KB 대비 64배 확대
     *   → 고부하 시 대용량 JSON 메시지 수용, 버퍼 오버플로우 방지
     * - asyncSendTimeout : 5,000ms — 느린 클라이언트에 대한 전송 타임아웃
     *   → 특정 세션이 무한정 블로킹되는 현상 차단
     * - maxSessionIdleTimeout : 300,000ms (5분) — 유휴 세션 자동 정리
     *   → 고아 세션(zombie session) 누적으로 인한 메모리 누수 방지
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(512 * 1024);   // 512 KB
        container.setMaxBinaryMessageBufferSize(512 * 1024); // 512 KB
        container.setAsyncSendTimeout(5_000L);               // 5 seconds
        container.setMaxSessionIdleTimeout(300_000L);        // 5 minutes
        return container;
    }
}