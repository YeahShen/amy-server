package site.ashenstation.amyserver.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        // 0L 表示永不超时，也可以设置具体的毫秒值
        SseEmitter emitter = new SseEmitter(0L);

        // 连接完成、超时或出错时，自动从列表中移除
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        emitters.add(emitter);
        return emitter;
    }

    public void sendToAll(Object message) {
        for (SseEmitter emitter : emitters) {
            try {
                // 构建并发送事件，可以设置id、事件名等
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("message")
                        .data(message));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    /**
     * 每 15 秒推送一条 SSE 注释（`: ping`）作为心跳，保持客户端连接活跃并及时探出断链。
     * 注释行符合 SSE 规范：eventsource-parser / 浏览器 EventSource 都会忽略，不触发 onmessage，
     * 因此不会干扰 axios-eventsource 的 `update:status` 事件流。
     */
    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
