package com.example.usedlion.service;

import com.example.usedlion.dto.ChatResponseDTO;
import com.example.usedlion.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

public interface ChatService {


    /**
     * 채팅방 입장 처리
     */
    void joinChatRoom(WebSocketSession session, Integer postId);

    /**
     * 채팅방 퇴장 처리
     */
    void leaveChatRoom(WebSocketSession session, Integer postId);

    /**
     * 특정 채팅방에 메시지 브로드캐스팅
     */
    void broadcastToPost(Integer postId, TextMessage message,  WebSocketSession senderSession);

    /**
     * URI에서 postId 추출
     */
    Integer extractPostId(WebSocketSession session);

    /**
     * 모든 채팅 내역 조회
     */
    List<Chat> getChatHistory(Integer postId);

    /**
     * 채팅내역 페이징 조회
     */
    Page<ChatResponseDTO> getChatPage(Integer postId, int page, Integer size);
}
