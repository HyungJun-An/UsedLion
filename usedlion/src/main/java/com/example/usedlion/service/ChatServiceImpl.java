package com.example.usedlion.service;

import com.example.usedlion.dto.ChatResponseDTO;
import com.example.usedlion.entity.Chat;
import com.example.usedlion.repository.ChatRepository;
import com.example.usedlion.repository.UserInformationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
@RequiredArgsConstructor
public class ChatServiceImpl extends TextWebSocketHandler implements ChatService {

    // postId별로 웹소켓 세션 그룹화 (채팅방)
    private final Map<Integer, Set<WebSocketSession>> chatRooms = new ConcurrentHashMap<>();

    // 1대1 채팅
    private final Map<String, Set<WebSocketSession>> privateRooms = new ConcurrentHashMap<>();

    // 세션 ID와 postId 매핑
    private final Map<String, Integer> sessionPostIdMap = new ConcurrentHashMap<>();
    
    // 세션 ID와 roomId 매핑
    private final Map<String, String> sessionRoomIdMap = new ConcurrentHashMap<>();

    // ID -> nickname 매핑 (G1)
    private final UserInformationRepository userInformationRepository;

    private final ChatRepository chatRepository;
    private final ObjectMapper objectMapper;

    /**
     * 클라이언트 연결 성립 시 호출되는 메서드
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("새로운 WebSocket 연결: {}", session.getId());

        String path = session.getUri().getPath();


        /**** 게시글 채팅 일 경우
         *
         */
        if (path.startsWith("/ws/chat/")) {

            // URI에서 postId 추출
            Integer postId = extractPostId(session);
            log.info("게시글 채팅방 입장: postId={}, sessionId={}", postId, session.getId());

            // 채팅방에 세션 추가
            joinChatRoom(session, postId);

            // 입장 메시지 브로드캐스팅
            Map<String, Object> message = new HashMap<>();
            message.put("type", "JOIN");
            message.put("postId", postId);
            message.put("sessionId", session.getId());
            message.put("message", "새로운 사용자가 입장했습니다.");
            message.put("timestamp", new Date());

            broadcastToPost(postId, new TextMessage(objectMapper.writeValueAsString(message)), session);

            // 유저 수 전달
            message = new HashMap<>();
            message.put("type", "STATUS");
            message.put("count", chatRooms.get(postId).size());

            broadcastToPost(postId, new TextMessage(objectMapper.writeValueAsString(message)), session);

            /**
             *  1 대 1 채팅 일 경우
             */
        } else {
            String roomId = extractRoomId(session);
            log.info("1대1 채팅방 입장: roomId={}, sessionId={}", roomId, session.getId());

            // 채팅방에 세션 추가
            joinPrivateRoom(session, roomId);

            // 입장 메시지 브로드캐스팅
            Map<String, Object> message = new HashMap<>();
            message.put("type", "JOIN");
            message.put("roomId", roomId);
            message.put("sessionId", session.getId());
            message.put("message", "상대방이 입장했습니다.");
            message.put("timestamp", new Date());

            broadcastToPrivate(roomId, new TextMessage(objectMapper.writeValueAsString(message)), session);

            // 유저 수 전달
            message = new HashMap<>();
            message.put("type", "STATUS");
            message.put("count", privateRooms.get(roomId).size());

            broadcastToPrivate(roomId, new TextMessage(objectMapper.writeValueAsString(message)), session);
        }
    }

    /**
     * 클라이언트가 메시지를 전송할 때 호출되는 메서드
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String payload = textMessage.getPayload();
        log.info("메시지 수신: {}", payload);

        try {
            Map<String, Object> messageMap = objectMapper.readValue(payload, Map.class);

            String type = (String) messageMap.get("type");
            log.info(messageMap.toString());
            if (type == null) {
                session.sendMessage(new TextMessage("{\"error\": \"type 필드가 누락되었습니다.\"}"));
                return;
            }

            switch (type) {
                case "MESSAGE":
                    handleNewMessage(session, messageMap);
                    break;
                case "EDIT":
                    handleEditMessage(session, messageMap);
                    break;
                case "DELETE":
                    handleDeleteMessage(session, messageMap);
                    break;
                case "P_MESSAGE":
                    handleNewPrivateMessage(session, messageMap);
                    break;
                case "P_EDIT":
                    handlePrivateEditMessage(session, messageMap);
                    break;
                case "P_DELETE":
                    handlePrivateDeleteMessage(session, messageMap);
                    break;
                default:
                    session.sendMessage(new TextMessage("{\"error\": \"알 수 없는 type입니다: " + type + "\"}"));
            }

        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
            session.sendMessage(new TextMessage("{\"error\": \"메시지 처리 중 오류가 발생했습니다: " + e.getMessage() + "\"}"));
        }
    }

    /**
     *  메세지 DB 저장 로직
     */
    private void handleNewMessage(WebSocketSession session, Map<String, Object> messageMap) throws IOException {
        Integer postId = Integer.parseInt(messageMap.get("postId").toString());
        Integer userId = Integer.parseInt(messageMap.get("senderId").toString());
        String content = messageMap.get("content").toString();

        Chat chat = new Chat(postId,null, userId, content);
        Chat savedMessage = chatRepository.save(chat);

        Map<String, Object> broadcastMessage = Map.of(
                "type", "MESSAGE",
                "id", savedMessage.getMsgId(),
                "postId", savedMessage.getPostId(),
                "userId", savedMessage.getUserId(),
                "content", savedMessage.getContent(),
                "timestamp", savedMessage.getCreatedAt()
        );

        broadcastToPost(postId, new TextMessage(objectMapper.writeValueAsString(broadcastMessage)), session);
    }

    /**
     * 메세지 DB 수정 로직
     */
    private void handleEditMessage(WebSocketSession session, Map<String, Object> messageMap) throws IOException {
            Integer msgId = Integer.parseInt(messageMap.get("msgId").toString());
            String newContent = messageMap.get("content").toString();

            Optional<Chat> optionalChat = chatRepository.findById(msgId);
            if (optionalChat.isPresent()) {
                Chat chat = optionalChat.get();
                chat.setContent(newContent);
                chatRepository.save(chat);

                Map<String, Object> editedMessage = Map.of(
                        "type", "EDITED",
                        "msgId", chat.getMsgId(),
                        "content", chat.getContent()
                );

                broadcastToPost(chat.getPostId(),new TextMessage(objectMapper.writeValueAsString(editedMessage)), session);
            } else {
                session.sendMessage(new TextMessage("{\"error\": \"메시지를 찾을 수 없습니다.\"}"));
            }
    }
    /**
     * 메세지 DB 삭제 로직
     */
    private void handleDeleteMessage(WebSocketSession session, Map<String, Object> messageMap) throws IOException {
        Integer msgId = Integer.parseInt(messageMap.get("msgId").toString());
        Optional<Chat> optionalChat = chatRepository.findById(msgId);
        log.info("msgId={}", msgId);
        if (optionalChat.isPresent()) {
            Chat chat = optionalChat.get();
            chatRepository.deleteById(msgId);
            Map<String, Object> deletedMessage = Map.of(
                    "type","DELETED",
                    "msgId", msgId
            );
            broadcastToPost(chat.getPostId(),new TextMessage(objectMapper.writeValueAsString(deletedMessage)), session);
        } else {
            session.sendMessage(new TextMessage("{\"error\": \"메시지를 찾을 수 없습니다.\"}"));
        }
    }
    /**
     * 1대1 채팅 내역 저장
     */
    private void handleNewPrivateMessage(WebSocketSession session, Map<String, Object> messageMap) throws IOException {
        String roomId = messageMap.get("roomId").toString();
        Integer userId = Integer.parseInt(messageMap.get("senderId").toString());
        String content = messageMap.get("content").toString();

        Chat chat = new Chat(null, roomId, userId, content);
        Chat savedMessage = chatRepository.save(chat);

        Map<String, Object> broadcastMessage = Map.of(
                "type", "MESSAGE",
                "id", savedMessage.getMsgId(),
                "roomId", savedMessage.getRoomId(),
                "userId", savedMessage.getUserId(),
                "content", savedMessage.getContent(),
                "timestamp", savedMessage.getCreatedAt()
        );

        broadcastToPrivate(roomId, new TextMessage(objectMapper.writeValueAsString(broadcastMessage)), session);
    }
    /**
     * 1대1 채팅 내역 수정
     */
    private void handlePrivateEditMessage(WebSocketSession session, Map<String, Object> messageMap) throws IOException {
        Integer msgId = Integer.parseInt(messageMap.get("msgId").toString());
        String newContent = messageMap.get("content").toString();

        Optional<Chat> optionalChat = chatRepository.findById(msgId);
        if (optionalChat.isPresent()) {
            Chat chat = optionalChat.get();
            chat.setContent(newContent);
            chatRepository.save(chat);

            Map<String, Object> editedMessage = Map.of(
                    "type", "EDITED",
                    "msgId", chat.getMsgId(),
                    "content", chat.getContent()
            );

            broadcastToPrivate(chat.getRoomId(),new TextMessage(objectMapper.writeValueAsString(editedMessage)), session);
        } else {
            session.sendMessage(new TextMessage("{\"error\": \"메시지를 찾을 수 없습니다.\"}"));
        }
    }

    /**
     * 메세지 DB 삭제 로직
     */
    private void handlePrivateDeleteMessage(WebSocketSession session, Map<String, Object> messageMap) throws IOException {
        Integer msgId = Integer.parseInt(messageMap.get("msgId").toString());
        Optional<Chat> optionalChat = chatRepository.findById(msgId);
        log.info("msgId={}", msgId);
        if (optionalChat.isPresent()) {
            Chat chat = optionalChat.get();
            chatRepository.deleteById(msgId);
            Map<String, Object> deletedMessage = Map.of(
                    "type","DELETED",
                    "msgId", msgId
            );
            broadcastToPrivate(chat.getRoomId(),new TextMessage(objectMapper.writeValueAsString(deletedMessage)), session);
        } else {
            session.sendMessage(new TextMessage("{\"error\": \"메시지를 찾을 수 없습니다.\"}"));
        }
    }

    /**
     * 클라이언트 연결 종료 시 호출되는 메서드
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        String path = session.getUri().getPath();

        /**** 게시글 채팅 일 경우
         *
         */
        if (path.startsWith("/ws/chat/")) {

            log.info("WebSocket 연결 종료: {}, 상태: {}", session.getId(), status);

            // 세션이 속한 채팅방 찾기
            Integer postId = sessionPostIdMap.get(session.getId());
            if (postId != null) {
                // 채팅방에서 세션 제거
                leaveChatRoom(session, postId);

                // 퇴장 메시지 브로드캐스팅
                Map<String, Object> message = new HashMap<>();
                message.put("type", "LEAVE");
                message.put("postId", postId);
                message.put("sessionId", session.getId());
                message.put("message", "사용자가 퇴장했습니다.");
                message.put("timestamp", new Date());

                broadcastToPost(postId, new TextMessage(objectMapper.writeValueAsString(message)), session);

                // 유저 수 전달
                message = new HashMap<>();
                message.put("type", "STATUS");
                message.put("count", chatRooms.get(postId).size());

                broadcastToPost(postId, new TextMessage(objectMapper.writeValueAsString(message)), session);
            }
        } else {
            log.info("WebSocket 연결 종료: {}, 상태: {}", session.getId(), status);

            // 세션이 속한 채팅방 찾기
            String roomId = sessionRoomIdMap.get(session.getId());
            if (roomId != null) {
                roomId = extractRoomId(session);
                // 채팅방에서 세션 제거
                leavePrivateRoom(session, roomId);

                // 퇴장 메시지 브로드캐스팅
                Map<String, Object> message = new HashMap<>();
                message.put("type", "LEAVE");
                message.put("postId", roomId);
                message.put("sessionId", session.getId());
                message.put("message", "사용자가 퇴장했습니다.");
                message.put("timestamp", new Date());

                broadcastToPrivate(roomId, new TextMessage(objectMapper.writeValueAsString(message)), session);

                // 유저 수 전달
                message = new HashMap<>();
                message.put("type", "STATUS");
                message.put("count", privateRooms.get(roomId).size());

                broadcastToPrivate(roomId, new TextMessage(objectMapper.writeValueAsString(message)), session);
            }
        }
    }

    /**
     * 예외 발생 시 호출되는 메서드
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 전송 오류: {}", session.getId(), exception);
    }

    /************************
    ;  사용자 정의 함수        ;
    ************************/

    /**
     * 게시글 채팅방 입장 처리
     */
    @Override
    public void joinChatRoom(WebSocketSession session, Integer postId) {
        // 채팅방이 없으면 새로 생성
        chatRooms.computeIfAbsent(postId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // 세션과 채팅방 매핑 저장
        sessionPostIdMap.put(session.getId(), postId);
    }

    /**
     * 1대1 채팅방 입장 처리
    */
    
    public void joinPrivateRoom(WebSocketSession session, String roomId) {
        // 채팅방이 없으면 새로 생성
        privateRooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // 세션과 채팅방 매핑 저장
        sessionRoomIdMap.put(session.getId(), roomId);

    }
     
    /**
     * 게시글 채팅방 퇴장 처리
     */
    @Override
    public void leaveChatRoom(WebSocketSession session, Integer postId) {
        Set<WebSocketSession> chatRoom = chatRooms.get(postId);
        if (chatRoom != null) {
            chatRoom.remove(session);

            // 채팅방이 비어있으면 제거
            if (chatRoom.isEmpty()) {
                chatRooms.remove(postId);
            }
        }

        // 세션 매핑 제거
        sessionPostIdMap.remove(session.getId());
    }

    /**
     * 1대1 채팅방 퇴장 처리
     */
    public void leavePrivateRoom(WebSocketSession session, String roomId) {
        Set<WebSocketSession> privateRoom = privateRooms.get(roomId);
        if (privateRoom != null) {
            privateRoom.remove(session);

            // 채팅방이 비어있으면 제거
            if (privateRoom.isEmpty()) {
                privateRooms.remove(roomId);
            }
        }

        // 세션 매핑 제거
        sessionRoomIdMap.remove(session.getId());
    }

    /**
     * 게시글 채팅방에 메시지 브로드캐스팅
     */
    @Override
    public void broadcastToPost(Integer postId, TextMessage message, WebSocketSession senderSession) {
        Set<WebSocketSession> sessions = chatRooms.get(postId);
        if (sessions != null) {
            sessions.forEach(session -> {
                try {
                      if(session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.error("메시지 전송 실패: {}", session.getId(), e);
                }
            });
        }
    }
    /**
     * 1대1 채팅방 메시지 브로드캐스팅
     */
    public void broadcastToPrivate(String roomId, TextMessage message, WebSocketSession senderSession) {
        Set<WebSocketSession> sessions = privateRooms.get(roomId);
        if (sessions != null) {
            sessions.forEach(session -> {
                try {
                    if(session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.error("메시지 전송 실패: {}", session.getId(), e);
                }
            });
        }
    }
    /**
     * URI에서 postId 추출
     */
    @Override
    public Integer extractPostId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        // /ws/chat/{postId} 형식에서 postId 추출
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("chat") && i + 1 < parts.length) {
                return Integer.parseInt(parts[i + 1]);
            }
        }
        throw new IllegalArgumentException("잘못된 URL 형식입니다");
    }
    /**
     * URI에서 userId 추출 후 roomId 생성
     */
    public String extractRoomId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        // /ws/private/chat/{userA}/{userB} 형식에서 roomId 추출
        for (int i = 0; i < parts.length; i++) {
            if ("private".equals(parts[i]) && i + 2 < parts.length) {
                int userA = Integer.parseInt(parts[i + 1]);
                int userB = Integer.parseInt(parts[i + 2]);
                if (userA < userB) {
                    return userA+"_"+userB;
                } else {
                    return userB+"_"+userA;
                }
            }
        }
        throw new IllegalArgumentException("잘못된 URL 형식입니다");
    }

    public List<Chat> getChatHistory(Integer postId) {
        // postId가 null이거나 유효하지 않은 값일 수 있으므로 예외 처리를 추가
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 postId 값입니다.");
        }
        return chatRepository.findByPostIdOrderByIdDesc(postId);  // DB 조회
    }

    public Page<ChatResponseDTO> getChatPage(Integer postId, int page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<Chat> chats = chatRepository.findByPostIdOrderByIdDesc(postId, pageRequest);

        return chats.map(chat -> {
            String nickname = userInformationRepository.findNicknameById((long) chat.getUserId());
            return new ChatResponseDTO(chat, nickname);
        });
    }


    public Page<ChatResponseDTO> getPrivateChatPage(String roomId, int page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<Chat> chats = chatRepository.findByRoomIdOrderByIdDesc(roomId, pageRequest);

        return chats.map(chat -> {
            String nickname = userInformationRepository.findNicknameById((long) chat.getUserId());
            return new ChatResponseDTO(chat, nickname);
        });
    }

}