package com.example.usedlion.chat;

import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.repository.UserInformationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatLoadTest {

    @LocalServerPort
    int port;

    @Autowired
    UserInformationRepository userRepo;

    static final int POST_ID = 1;
    static final int TOTAL_USERS = 30;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ────────────────────────────────────────
    // Setup / Teardown
    // ────────────────────────────────────────

    @BeforeEach
    void setupUsers() {
        for (int i = 1; i <= TOTAL_USERS; i++) {
            String email = "loadtest" + i + "@test.com";
            UserInformation existing = userRepo.findByEmail(email);
            if (existing != null) continue;

            UserInformation user = new UserInformation();
            user.setEmail(email);
            user.setPassword("$2a$10$hashedpassword");
            user.setUsername("loaduser" + i);
            user.setNickname("부하유저" + i);
            user.setProvider("local");
            user.setProviderId(null);
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now());
            user.setRegion("서울");
            userRepo.save(user);
        }
    }

    // ────────────────────────────────────────
    // 테스트 1: 1명 유저, 100개 메시지 기준선
    // ────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[부하테스트] 1명 유저 - 100개 메시지 기준선")
    void load_1user_100messages() throws Exception {
        int messages = 100;
        LoadResult result = runLoad(1, messages);

        System.out.println("\n═══ [1 유저 / 100 메시지] ═══");
        printResult(result);

        assertThat(result.errorCount.get()).isEqualTo(0);
        assertThat(result.sentCount.get()).isEqualTo(messages);
    }

    // ────────────────────────────────────────
    // 테스트 2: 10명 동시, 각 50개 메시지
    // ────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("[부하테스트] 10명 동시 - 각 50개 메시지")
    void load_10users_concurrent() throws Exception {
        int users = 10;
        int messagesPerUser = 50;
        LoadResult result = runLoad(users, messagesPerUser);

        System.out.println("\n═══ [10 유저 / 각 50 메시지] ═══");
        printResult(result);

        assertThat(result.errorCount.get()).isEqualTo(0);
        assertThat(result.sentCount.get()).isEqualTo(users * messagesPerUser);
    }

    // ────────────────────────────────────────
    // 테스트 3: 30명 동시, 각 20개 메시지
    // ────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("[부하테스트] 30명 동시 - 각 20개 메시지")
    void load_30users_concurrent() throws Exception {
        int users = 30;
        int messagesPerUser = 20;
        LoadResult result = runLoad(users, messagesPerUser);

        System.out.println("\n═══ [30 유저 / 각 20 메시지] ═══");
        printResult(result);

        int total = users * messagesPerUser;
        double errorRate = (double) result.errorCount.get() / total * 100;
        System.out.printf("에러율: %.1f%%%n", errorRate);
        assertThat(errorRate).isLessThan(5.0);
    }

    // ────────────────────────────────────────
    // 테스트 4: 동시성 - 30명 동일 채팅방 브로드캐스트
    // ────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("[동시성] 30명 동일 채팅방 동시 브로드캐스트 - 에러 감지")
    void concurrency_30users_sameChatRoom() throws Exception {
        int users = 30;
        int messagesPerUser = 10;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(users);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger sentCount = new AtomicInteger(0);

        List<WebSocket> sockets = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(users);

        for (int i = 0; i < users; i++) {
            HttpClient client = HttpClient.newHttpClient();
            AtomicInteger recvCount = new AtomicInteger(0);
            WebSocket ws = client.newWebSocketBuilder()
                    .buildAsync(
                            URI.create("ws://localhost:8080" + "/ws/chat/" + POST_ID),
                            new SimpleListener(recvCount)
                    ).join();
            sockets.add(ws);
        }

        Thread.sleep(300);

        for (int i = 0; i < users; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    WebSocket ws = sockets.get(idx);
                    for (int m = 0; m < messagesPerUser; m++) {
                        try {
                            String msg = String.format(
                                    "{\"type\":\"MESSAGE\",\"postId\":%d,\"senderId\":%d,\"content\":\"동시테스트 유저%d 메시지%d\",\"timestamp\":\"%s\"}",
                                    POST_ID, idx + 1, idx + 1, m, Instant.now().toString()
                            );
                            ws.sendText(msg, true).join();
                            sentCount.incrementAndGet();
                            Thread.sleep(10);
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean done = doneLatch.await(30, TimeUnit.SECONDS);

        System.out.println("\n═══ [동시성 30명 동일 채팅방] ═══");
        System.out.println("전송 성공: " + sentCount.get());
        System.out.println("에러 수: " + errorCount.get());
        System.out.println("타임아웃: " + !done);

        sockets.forEach(WebSocket::abort);
        executor.shutdown();

        double errorRate = (double) errorCount.get() / (users * messagesPerUser) * 100;
        System.out.printf("에러율: %.1f%%%n", errorRate);
        if (errorCount.get() > 0) {
            System.out.println("⚠  sendMessage() 동시성 이슈 감지 → synchronized 블록 또는 메시지 큐 도입 필요");
        }
    }

    // ────────────────────────────────────────
    // 테스트 5: 30명 × 10 메시지 수신 완전성 + 실시간 가시성 검증
    // ────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("[수신 완전성+가시성] 30명 × 10 메시지 → 각 유저 300개 수신 및 크로스-유저 전달 확인")
    void broadcast_completeness_30users_10messages() throws Exception {
        int users = 30;
        int messagesPerUser = 10;
        int totalMessages = users * messagesPerUser; // 300개

        // 각 리스너: 총 수신 카운터, 유저별 수신 맵, 타입별 카운터
        MessageCountingListener[] listeners = new MessageCountingListener[users];
        // 9,000 이벤트(30명 × 300개) 모두 수신 시 latch 완료
        CountDownLatch allReceived = new CountDownLatch(users * totalMessages);

        List<WebSocket> sockets = new CopyOnWriteArrayList<>();

        for (int i = 0; i < users; i++) {
            listeners[i] = new MessageCountingListener(i + 1, allReceived);
            HttpClient client = HttpClient.newHttpClient();
            WebSocket ws = client.newWebSocketBuilder()
                    .buildAsync(
                            URI.create("ws://localhost:8080" + "/ws/chat/" + POST_ID),
                            listeners[i]
                    ).join();
            sockets.add(ws);
            Thread.sleep(30); // 연결 안정화
        }

        Thread.sleep(500);

        CountDownLatch sendDone = new CountDownLatch(users);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(users);
        AtomicInteger sendError = new AtomicInteger(0);

        for (int i = 0; i < users; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int m = 0; m < messagesPerUser; m++) {
                        try {
                            // content 형식: "user{N}-msg{M}" → 서버가 그대로 저장 후 브로드캐스트
                            String msg = String.format(
                                    "{\"type\":\"MESSAGE\",\"postId\":%d,\"senderId\":%d,\"content\":\"user%d-msg%d\",\"timestamp\":\"%s\"}",
                                    POST_ID, idx + 1, idx + 1, m, Instant.now().toString()
                            );
                            sockets.get(idx).sendText(msg, true).join();
                            Thread.sleep(20);
                        } catch (Exception e) {
                            sendError.incrementAndGet();
                            System.out.printf("[SEND ERROR] 유저%d 메시지%d: %s%n", idx + 1, 0, e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    sendDone.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean sendFinished = sendDone.await(30, TimeUnit.SECONDS);
        if (!sendFinished) {
            System.out.println("⚠  전송 단계 타임아웃 발생");
        }

        // DB 저장 + 브로드캐스트 완료 대기 (60초 — DB 저장 지연 + synchronized 브로드캐스트 반영)
        boolean allDone = allReceived.await(60, TimeUnit.SECONDS);

        System.out.println("\n═══ [수신 완전성 + 실시간 가시성 검증] ═══");
        System.out.printf("총 전송: %d개, 전송 에러: %d개%n", totalMessages, sendError.get());
        System.out.printf("기대 수신: 유저당 %d개 (전체 %d개)%n", totalMessages, (long) users * totalMessages);
        System.out.println();

        // ── 타입별 전체 통계 ──
        Map<String, Integer> globalTypeCounts = new LinkedHashMap<>();
        for (MessageCountingListener l : listeners) {
            l.getTypeCounts().forEach((type, cnt) ->
                    globalTypeCounts.merge(type, cnt.get(), Integer::sum));
        }
        System.out.println("── 메시지 타입별 수신 통계 (전체 합산) ──");
        globalTypeCounts.forEach((type, cnt) ->
                System.out.printf("  %-10s : %d%n", type, cnt));
        System.out.println();

        // ── 유저별 수신 현황 ──
        System.out.println("── 유저별 MESSAGE 수신 현황 ──");
        int failCount = 0;
        for (int i = 0; i < users; i++) {
            int recv = listeners[i].getMessageCount();
            boolean ok = recv == totalMessages;
            if (!ok) {
                failCount++;
                System.out.printf("  유저 %2d: %3d/%d ✗ MISSING %d%n",
                        i + 1, recv, totalMessages, totalMessages - recv);
            } else {
                System.out.printf("  유저 %2d: %3d/%d ✓%n", i + 1, recv, totalMessages);
            }
        }
        System.out.println();

        // ── 크로스-유저 가시성 검증 ──
        System.out.println("── 크로스-유저 가시성 (다른 유저 메시지 수신 여부) ──");
        int visibilityFailCount = 0;
        for (int i = 0; i < users; i++) {
            int myId = i + 1;
            Map<Integer, Integer> perSender = listeners[i].getPerSenderCounts();
            int distinctSenders = perSender.size();
            int othersReceived = perSender.entrySet().stream()
                    .filter(e -> e.getKey() != myId)
                    .mapToInt(Map.Entry::getValue)
                    .sum();
            boolean crossOk = distinctSenders > 1 && othersReceived > 0;
            if (!crossOk) {
                visibilityFailCount++;
                System.out.printf("  유저 %2d: 수신 발신자 수=%d, 타인 메시지=%d ✗ 가시성 실패%n",
                        myId, distinctSenders, othersReceived);
            } else {
                System.out.printf("  유저 %2d: 수신 발신자 수=%d (자신 포함), 타인 메시지=%d ✓%n",
                        myId, distinctSenders, othersReceived);
            }
        }
        System.out.println();

        System.out.printf("수신 완전성: %d명 완전 수신 / %d명 미수신%n", users - failCount, failCount);
        System.out.printf("크로스-가시성: %d명 성공 / %d명 실패%n", users - visibilityFailCount, visibilityFailCount);

        sockets.forEach(WebSocket::abort);
        executor.shutdown();

        // ── 어서션 ──
        assertThat(sendError.get())
                .as("전송 에러 0건이어야 함")
                .isEqualTo(0);
        assertThat(allDone)
                .as("60초 내 모든 메시지(%d개) 수신 실패 — latch 잔여: %d",
                        (long) users * totalMessages, allReceived.getCount())
                .isTrue();
        for (int i = 0; i < users; i++) {
            assertThat(listeners[i].getMessageCount())
                    .as("유저 %d 수신 MESSAGE 개수", i + 1)
                    .isEqualTo(totalMessages);
        }
        for (int i = 0; i < users; i++) {
            int myId = i + 1;
            Map<Integer, Integer> perSender = listeners[i].getPerSenderCounts();
            int othersReceived = perSender.entrySet().stream()
                    .filter(e -> e.getKey() != myId)
                    .mapToInt(Map.Entry::getValue)
                    .sum();
            assertThat(othersReceived)
                    .as("유저 %d는 타인이 보낸 메시지를 최소 1개 이상 수신해야 함 (실시간 가시성)", myId)
                    .isGreaterThan(0);
        }
    }

    // ────────────────────────────────────────
    // 공통 부하 실행 메서드
    // ────────────────────────────────────────

    private LoadResult runLoad(int userCount, int messagesPerUser) throws Exception {
        LoadResult result = new LoadResult();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(userCount);
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        List<WebSocket> sockets = new CopyOnWriteArrayList<>();

        for (int i = 0; i < userCount; i++) {
            HttpClient client = HttpClient.newHttpClient();
            WebSocket ws = client.newWebSocketBuilder()
                    .buildAsync(
                            URI.create("ws://localhost:8080" + "/ws/chat/" + POST_ID),
                            new SimpleListener(result.receivedCount)
                    ).join();
            sockets.add(ws);
        }

        Thread.sleep(200);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < userCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    WebSocket ws = sockets.get(idx);
                    for (int m = 0; m < messagesPerUser; m++) {
                        try {
                            String msg = String.format(
                                    "{\"type\":\"MESSAGE\",\"postId\":%d,\"senderId\":%d,\"content\":\"부하테스트 유저%d 메시지%d\",\"timestamp\":\"%s\"}",
                                    POST_ID, idx + 1, idx + 1, m, Instant.now().toString()
                            );
                            ws.sendText(msg, true).join();
                            result.sentCount.incrementAndGet();
                        } catch (Exception e) {
                            result.errorCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        result.elapsedMs = System.currentTimeMillis() - startTime;

        sockets.forEach(WebSocket::abort);
        executor.shutdown();
        return result;
    }

    private void printResult(LoadResult r) {
        int total = r.sentCount.get() + r.errorCount.get();
        double throughput = total > 0 ? r.sentCount.get() / (r.elapsedMs / 1000.0) : 0;
        System.out.printf("전송 성공  : %d개%n", r.sentCount.get());
        System.out.printf("에러      : %d개%n", r.errorCount.get());
        System.out.printf("소요 시간  : %dms%n", r.elapsedMs);
        System.out.printf("처리량    : %.1f msg/sec%n", throughput);
    }

    // ────────────────────────────────────────
    // 헬퍼 클래스
    // ────────────────────────────────────────

    static class LoadResult {
        AtomicInteger sentCount = new AtomicInteger(0);
        AtomicInteger receivedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        long elapsedMs = 0;
    }

    // ────────────────────────────────────────
    // SimpleListener: 카운트만 하는 단순 리스너
    // ────────────────────────────────────────

    static class SimpleListener implements WebSocket.Listener {
        private final AtomicInteger counter;
        // 프래그먼트 버퍼 (last=false 대비)
        private final StringBuilder buffer = new StringBuilder();

        SimpleListener(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                counter.incrementAndGet();
                buffer.setLength(0);
            }
            // 프래그먼트 여부와 관계없이 항상 다음 프레임을 요청
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            // 연결 종료 시 발생하는 에러는 무시
        }
    }

    // ────────────────────────────────────────
    // MessageCountingListener: 완전성 + 가시성 검증용 리스너
    // ────────────────────────────────────────

    static class MessageCountingListener implements WebSocket.Listener {
        private final int listenerId;
        private final CountDownLatch latch;

        // 프래그먼트 버퍼 (last=false 대비)
        private final StringBuilder buffer = new StringBuilder();

        // MESSAGE 타입 총 수신 수
        private final AtomicInteger messageCount = new AtomicInteger(0);

        // 발신자(userId)별 수신 수 — 크로스-유저 가시성 검증용
        // 서버는 senderId가 아닌 userId 필드로 브로드캐스트함
        private final ConcurrentHashMap<Integer, AtomicInteger> perSenderCounts = new ConcurrentHashMap<>();

        // 메시지 타입별 카운터 — 지연 원인 파악용
        private final ConcurrentHashMap<String, AtomicInteger> typeCounts = new ConcurrentHashMap<>();

        MessageCountingListener(int listenerId, CountDownLatch latch) {
            this.listenerId = listenerId;
            this.latch = latch;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);

            if (!last) {
                // 아직 프래그먼트가 더 남아있음 — 다음 프레임 요청 후 대기
                webSocket.request(1);
                return null;
            }

            // 완전한 메시지 수신 → 처리
            String fullMessage = buffer.toString();
            buffer.setLength(0);

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = MAPPER.readValue(fullMessage, Map.class);
                String type = (String) msg.get("type");

                if (type == null) {
                    System.out.printf("[LISTENER %d] type 필드 없는 메시지: %s%n", listenerId, fullMessage);
                    webSocket.request(1);
                    return null;
                }

                // 타입별 카운터 증가
                typeCounts.computeIfAbsent(type, k -> new AtomicInteger(0)).incrementAndGet();

                if ("MESSAGE".equals(type)) {
                    // userId: 서버가 브로드캐스트 시 senderId 대신 userId로 전달
                    Object userIdObj = msg.get("userId");
                    if (userIdObj != null) {
                        int senderId = Integer.parseInt(userIdObj.toString());
                        perSenderCounts.computeIfAbsent(senderId, k -> new AtomicInteger(0))
                                .incrementAndGet();
                    } else {
                        // userId 필드가 없을 경우 진단 로그
                        System.out.printf("[LISTENER %d] MESSAGE 타입에 userId 필드 없음: %s%n",
                                listenerId, fullMessage);
                    }

                    messageCount.incrementAndGet();
                    latch.countDown();

                } else if ("JOIN".equals(type) || "LEAVE".equals(type) || "STATUS".equals(type)) {
                    // 시스템 메시지는 카운트하지 않음 — 타입 통계에만 반영됨
                } else {
                    System.out.printf("[LISTENER %d] 알 수 없는 타입: %s%n", listenerId, type);
                }

            } catch (Exception e) {
                System.out.printf("[LISTENER %d] JSON 파싱 실패 (%s): %s%n",
                        listenerId, e.getMessage(), fullMessage);
            }

            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            // 연결 종료 시 발생하는 에러는 무시
        }

        int getMessageCount() {
            return messageCount.get();
        }

        Map<Integer, Integer> getPerSenderCounts() {
            Map<Integer, Integer> result = new LinkedHashMap<>();
            perSenderCounts.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }

        Map<String, AtomicInteger> getTypeCounts() {
            return typeCounts;
        }
    }
}
