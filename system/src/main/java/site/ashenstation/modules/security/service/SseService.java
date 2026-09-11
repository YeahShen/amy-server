package site.ashenstation.modules.security.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.ashenstation.modules.security.vo.NotificationVO;
import site.ashenstation.utils.SecurityUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {
    public static final Map<String, SseEmitter> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    public SseEmitter createEmitter() {
        // 0L 表示永不超时，也可以设置具体的毫秒值
        SseEmitter emitter = new SseEmitter(0L);

        String currentUserId = SecurityUtils.getCurrentUserId();
        String tokenUid = SecurityUtils.getTokenUid();


        // 连接完成、超时或出错时，自动从列表中移除
        emitter.onCompletion(() -> {
            ONLINE_SESSIONS.remove(currentUserId + ":" + tokenUid);
        });
        emitter.onTimeout(() -> {
            ONLINE_SESSIONS.remove(currentUserId + ":" + tokenUid);
        });
        emitter.onError((e) -> {
            ONLINE_SESSIONS.remove(currentUserId + ":" + tokenUid);
        });


        ONLINE_SESSIONS.put(currentUserId + ":" + tokenUid, emitter);

        return emitter;
    }

    public void sendToAll(Object message) {
        ONLINE_SESSIONS.entrySet().stream().forEach(entry -> {
            SseEmitter emitter = entry.getValue();

            try {
                // 构建并发送事件，可以设置id、事件名等
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("message")
                        .data(message));
            } catch (IOException e) {
                ONLINE_SESSIONS.remove(entry.getKey());
            }
        });
    }

    public void SendMessage(String id, NotificationVO message) {
        SseEmitter emitter = ONLINE_SESSIONS.get(id);
        try {
            // 构建并发送事件，可以设置id、事件名等
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name("message")
                    .data(message));
        } catch (IOException e) {
            ONLINE_SESSIONS.remove(id);
        }
    }

    /**
     * 每 15 秒推送一条 SSE 注释（`: ping`）作为心跳，保持客户端连接活跃并及时探出断链。
     * 注释行符合 SSE 规范：eventsource-parser / 浏览器 EventSource 都会忽略，不触发 onmessage，
     * 因此不会干扰 axios-eventsource 的 `update:status` 事件流。
     */
    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        ONLINE_SESSIONS.entrySet().stream().forEach(entry -> {
            SseEmitter emitter = entry.getValue();

            try {
                // 构建并发送事件，可以设置id、事件名等
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException e) {
                ONLINE_SESSIONS.remove(entry.getKey());
            }
        });
    }
}