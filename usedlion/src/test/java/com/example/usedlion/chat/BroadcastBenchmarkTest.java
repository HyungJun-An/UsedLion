package com.example.usedlion.chat;

import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.repository.UserInformationRepository;
import com.example.usedlion.service.ChatServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ═══════════════════════════════════════════════════════════
 *  브로드캐스트 최적화 실측 비교 벤치마크
 * ═══════════════════════════════════════════════════════════
 *
 *  측정 시나리오: Head-of-Line Blocking 재현
 *  ─ 1명의 발신자가 메시지를 연속 전송 (sleep 없음)
 *  ─ 30명의 수신자가 모든 메시지 수신 대기
 *  ─ 세션당 2ms 지연으로 실제 네트워크 레이턴시 시뮬레이션
 *
 *  개선 전 (순차 forEach):
 *    핸들러 스레드가 30 × 2ms = 60ms 블로킹
 *    → 다음 메시지가 큐에 쌓임
 *    → N 메시지 × 60ms = 직렬 처리 병목
 *
 *  개선 후 (CompletableFuture 병렬):
 *    핸들러 스레드가 태스크 제출 후 즉시 반환 (~1ms)
 *    → 다음 메시지 즉시 처리
 *    → 브로드캐스트 실행은 executor 에서 병렬 처리
 * ═══════════════════════════════════════════════════════════
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "chat.broadcast.simulated.delay.ms=2"  // 세션당 2ms 네트워크 레이턴시 시뮬레이션
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BroadcastBenchmarkTest {

    @LocalServerPort
    int port;

    @Autowired
    ChatServiceImpl chatService;

    @Autowired
    UserInformationRepository userRepo;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int POST_ID   = 999;
    private static final int RECEIVERS = 50;   // 수신 클라이언트 수
    private static final int MSG_COUNT = 40;   // 발신자 1명이 연속 전송할 메시지 수

    private static BenchmarkResult legacyResult;
    private static BenchmarkResult parallelResult;

    // ── 유저 세팅 ────────────────────────────────────────

    @BeforeEach
    void setupUsers() {
        for (int i = 1; i <= RECEIVERS + 1; i++) {
            String email = "bench" + i + "@test.com";
            if (userRepo.findByEmail(email) != null) continue;
            UserInformation u = new UserInformation();
            u.setEmail(email);
            u.setPassword("$2a$10$hashedpassword");
            u.setUsername("benchuser" + i);
            u.setNickname("벤치유저" + i);
            u.setProvider("local");
            u.setRole("USER");
            u.setCreatedAt(LocalDateTime.now());
            u.setRegion("서울");
            userRepo.save(u);
        }
    }

    // ── Phase 0: JVM 워밍업 ──────────────────────────────

    @Test
    @Order(0)
    @DisplayName("[Phase 0] JVM 워밍업 (측정 제외)")
    void phase0_warmup() throws Exception {
        System.out.println("\n▶ Phase 0: JVM 워밍업 (5명 × 5 메시지)");
        setLegacyMode(false);
        runBenchmark(5, 5, POST_ID + 10);
        System.out.println("✓ 워밍업 완료");
    }

    // ── Phase 1: 개선 전 ─────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[Phase 1] 개선 전 — forEach 순차 브로드캐스트 (HoL Blocking 재현)")
    void phase1_legacy_sequential() throws Exception {
        setLegacyMode(true);
        System.out.println("\n▶ Phase 1: forEach 순차 브로드캐스트");
        System.out.printf("  조건: 수신자 %d명, 메시지 %d개, 세션당 %dms 지연%n",
                RECEIVERS, MSG_COUNT, 2);
        System.out.printf("  예상 브로드캐스트 1회: %d명 × 2ms = %dms (핸들러 스레드 블로킹)%n",
                RECEIVERS, RECEIVERS * 2);
        legacyResult = runBenchmark(RECEIVERS, MSG_COUNT, POST_ID);
        printResult("개선 전 (순차 forEach)", legacyResult);
    }

    // ── Phase 2: 개선 후 ─────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("[Phase 2] 개선 후 — CompletableFuture 병렬 브로드캐스트")
    void phase2_optimized_parallel() throws Exception {
        setLegacyMode(false);
        System.out.println("\n▶ Phase 2: CompletableFuture 병렬 브로드캐스트");
        System.out.printf("  조건: 수신자 %d명, 메시지 %d개, 세션당 %dms 지연%n",
                RECEIVERS, MSG_COUNT, 2);
        System.out.printf("  예상 브로드캐스트 1회: ~2ms (핸들러 스레드 즉시 반환)%n");
        parallelResult = runBenchmark(RECEIVERS, MSG_COUNT, POST_ID + 1);
        printResult("개선 후 (CompletableFuture 병렬)", parallelResult);
    }

    // ── Phase 3: 비교 리포트 ─────────────────────────────

    @Test
    @Order(3)
    @DisplayName("[Phase 3] 개선 전/후 실측 비교 리포트")
    void phase3_comparison_report() {
        assertThat(legacyResult).isNotNull();
        assertThat(parallelResult).isNotNull();

        long   totalEvents   = (long) RECEIVERS * MSG_COUNT;
        double latencyDiff   = legacyResult.elapsedMs - parallelResult.elapsedMs;
        double latencyPct    = latencyDiff / legacyResult.elapsedMs * 100;
        double throughputPct = (parallelResult.throughput - legacyResult.throughput)
                               / legacyResult.throughput * 100;

        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║         WebSocket 브로드캐스트 최적화 — 실측 성능 비교 리포트               ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  시나리오: 발신자 1명 × 메시지 %d개, 수신자 %d명 (총 %,d 이벤트)%n",
                MSG_COUNT, RECEIVERS, totalEvents);
        System.out.println("║  지연 조건: 세션당 2ms (실제 네트워크 레이턴시 시뮬레이션)                  ║");
        System.out.println("╠═════════════════════════════╦══════════════════╦═════════════════════════╣");
        System.out.println("║            지표             ║  개선 전 (순차)  ║  개선 후 (병렬)          ║");
        System.out.println("╠═════════════════════════════╬══════════════════╬═════════════════════════╣");
        System.out.printf( "║  전체 처리 시간             ║  %9d ms   ║  %9d ms              ║%n",
                legacyResult.elapsedMs, parallelResult.elapsedMs);
        System.out.printf( "║  메시지당 평균 처리 시간    ║  %9.1f ms   ║  %9.1f ms              ║%n",
                (double) legacyResult.elapsedMs  / MSG_COUNT,
                (double) parallelResult.elapsedMs / MSG_COUNT);
        System.out.printf( "║  처리량 (event/sec)         ║  %9.1f      ║  %9.1f               ║%n",
                legacyResult.throughput, parallelResult.throughput);
        System.out.printf( "║  수신 완료 이벤트           ║  %,9d      ║  %,9d               ║%n",
                legacyResult.received, parallelResult.received);
        System.out.printf( "║  유실 이벤트                ║  %9d      ║  %9d               ║%n",
                legacyResult.missed, parallelResult.missed);
        System.out.printf( "║  타임아웃                   ║  %9s      ║  %9s               ║%n",
                legacyResult.timedOut  ? "YES ⚠" : "NO  ✓",
                parallelResult.timedOut ? "YES ⚠" : "NO  ✓");
        System.out.println("╠═════════════════════════════╩══════════════════╩═════════════════════════╣");
        System.out.printf( "║  ▶ 처리 시간 단축: %.0f ms (%.1f%% 감소)%n", latencyDiff, latencyPct);
        System.out.printf( "║  ▶ 처리량 향상:  +%.1f%% (%.0f → %.0f event/sec)%n",
                throughputPct, legacyResult.throughput, parallelResult.throughput);
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  [분석] 개선 전: 핸들러 스레드가 30세션×2ms=60ms 블로킹 → 다음 메시지 대기 큐 형성");
        System.out.println("  [분석] 개선 후: 핸들러 스레드가 ~1ms 후 즉시 반환 → 다음 메시지 즉시 처리");
        System.out.println("  [측정 환경] localhost + 2ms 인위적 지연 (실제 네트워크 RTT 10~50ms 시 효과 더 큼)");

        // 핵심 검증: 병렬 방식의 정확성 보장
        assertThat(parallelResult.missed)
                .as("개선 후 — 유실 이벤트 0건 (메시지 완전성 보장)")
                .isEqualTo(0);
        assertThat(parallelResult.timedOut)
                .as("개선 후 — 60초 내 완료")
                .isFalse();
        // 처리 시간 비교 (HoL Blocking 제거 효과)
        assertThat(parallelResult.elapsedMs)
                .as("개선 후 처리 시간이 개선 전보다 단축되어야 함 (HoL Blocking 제거 효과)")
                .isLessThan(legacyResult.elapsedMs);
    }

    // ── 벤치마크 로직 ─────────────────────────────────────

    /**
     * @param receivers 수신 클라이언트 수
     * @param msgCount  발신자 1명이 연속 전송할 메시지 수 (sleep 없음)
     * @param postId    테스트 채팅방 ID
     */
    private BenchmarkResult runBenchmark(int receivers, int msgCount, int postId) throws Exception {
        int expectedEvents = receivers * msgCount;
        CountDownLatch allReceived = new CountDownLatch(expectedEvents);
        AtomicInteger[] perReceiver = new AtomicInteger[receivers];
        List<WebSocket> sockets = new CopyOnWriteArrayList<>();

        // 수신자 연결
        for (int i = 0; i < receivers; i++) {
            perReceiver[i] = new AtomicInteger(0);
            final int idx = i;
            WebSocket ws = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(
                            URI.create("ws://localhost:" + port + "/ws/chat/" + postId),
                            new CountingListener(idx + 1, perReceiver[idx], allReceived))
                    .join();
            sockets.add(ws);
            Thread.sleep(20);
        }

        // 발신자도 동일 채팅방에 연결 (브로드캐스트 대상에 포함되므로 수신자 수와 동일한 조건)
        WebSocket sender = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(
                        URI.create("ws://localhost:" + port + "/ws/chat/" + postId),
                        new CountingListener(0, new AtomicInteger(), new CountDownLatch(1)))
                .join();
        Thread.sleep(500); // 전체 연결 안정화

        long startMs = System.currentTimeMillis();

        // 메시지 연속 전송 (sleep 없음 — HoL Blocking 재현)
        AtomicInteger sendErrors = new AtomicInteger(0);
        for (int m = 0; m < msgCount; m++) {
            try {
                String msg = String.format(
                        "{\"type\":\"MESSAGE\",\"postId\":%d,\"senderId\":1," +
                        "\"content\":\"hol-test-msg%d\",\"timestamp\":\"%s\"}",
                        postId, m, Instant.now());
                sender.sendText(msg, true).join();
            } catch (Exception e) {
                sendErrors.incrementAndGet();
            }
        }

        boolean timedOut = !allReceived.await(60, TimeUnit.SECONDS);
        long elapsedMs   = System.currentTimeMillis() - startMs;

        sender.abort();
        sockets.forEach(WebSocket::abort);

        int totalReceived = Arrays.stream(perReceiver).mapToInt(AtomicInteger::get).sum();
        int missed = expectedEvents - totalReceived;

        BenchmarkResult r = new BenchmarkResult();
        r.elapsedMs  = elapsedMs;
        r.received   = totalReceived;
        r.missed     = missed;
        r.sendErrors = sendErrors.get();
        r.timedOut   = timedOut;
        r.throughput = totalReceived / (elapsedMs / 1000.0);
        r.perReceiver = perReceiver;
        return r;
    }

    private void printResult(String label, BenchmarkResult r) {
        System.out.printf("%n── %s 결과 ──%n", label);
        System.out.printf("  처리 시간      : %,d ms%n", r.elapsedMs);
        System.out.printf("  처리량         : %.1f event/sec%n", r.throughput);
        System.out.printf("  수신 / 기대    : %,d / %,d%n", r.received, (long) RECEIVERS * MSG_COUNT);
        System.out.printf("  유실           : %d%n", r.missed);
        System.out.printf("  타임아웃       : %s%n", r.timedOut ? "발생 ⚠" : "없음 ✓");
        int fails = 0;
        for (int i = 0; i < r.perReceiver.length; i++) {
            int got = r.perReceiver[i].get();
            if (got != MSG_COUNT) {
                fails++;
                System.out.printf("  [누락] 수신자 %2d: %d/%d (-%d)%n",
                        i + 1, got, MSG_COUNT, MSG_COUNT - got);
            }
        }
        if (fails == 0) System.out.println("  [OK] 모든 수신자 완전 수신");
    }

    // ── 리플렉션으로 legacyBroadcast 플래그 토글 ──────────

    private void setLegacyMode(boolean legacy) throws Exception {
        Field f = ChatServiceImpl.class.getDeclaredField("legacyBroadcast");
        f.setAccessible(true);
        f.set(chatService, legacy);
        System.out.printf("[설정] legacyBroadcast = %b%n", legacy);
    }

    // ── 결과 DTO ──────────────────────────────────────────

    static class BenchmarkResult {
        long  elapsedMs;
        int   received;
        int   missed;
        int   sendErrors;
        boolean timedOut;
        double  throughput;
        AtomicInteger[] perReceiver;
    }

    // ── WebSocket 리스너 ───────────────────────────────────

    static class CountingListener implements WebSocket.Listener {
        private final int id;
        private final AtomicInteger counter;
        private final CountDownLatch latch;
        private final StringBuilder buffer = new StringBuilder();

        CountingListener(int id, AtomicInteger counter, CountDownLatch latch) {
            this.id      = id;
            this.counter = counter;
            this.latch   = latch;
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (!last) { ws.request(1); return null; }
            String full = buffer.toString();
            buffer.setLength(0);
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = MAPPER.readValue(full, Map.class);
                if ("MESSAGE".equals(msg.get("type"))) {
                    counter.incrementAndGet();
                    latch.countDown();
                }
            } catch (Exception e) {
                System.out.printf("[R%d] 파싱 오류: %s%n", id, e.getMessage());
            }
            ws.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) { /* 종료 시 무시 */ }
    }
}
