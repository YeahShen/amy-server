package site.ashenstation.amyserver.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import site.ashenstation.amyserver.dto.CreateVideoDto;
import site.ashenstation.amyserver.dto.SseMessageDto;
import site.ashenstation.amyserver.dto.UploadTaskDto;
import site.ashenstation.amyserver.enums.SseMessageEvent;
import site.ashenstation.amyserver.enums.UploadTaskType;
import site.ashenstation.amyserver.property.StaticResourceDirectoryProperties;
import site.ashenstation.amyserver.utils.FFMpegUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 上传完成后的异步处理服务。
 * 独立成 bean 是为了让 {@code @Async} 经由 Spring 代理生效
 * （从 UploadService 内部直接调用会因自调用问题而同步执行）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadProcessService {

    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;
    private final SseService sseService;
    private final FFMpegUtils fFmpegUtils;
    private final VideoService videoService;

    @Async("customTaskExecutor")
    public void nextStepDeal(String id, String emitterId) {
        try {
            Path uploadDir = Paths.get(staticResourceDirectoryProperties.getUploadTempDirectory(), id);

            // 任务被处理过（临时目录已清理）则直接跳过，保证接口可重复调用
            if (!Files.exists(uploadDir)) {
                log.warn("上传任务 {} 不存在或已处理完毕", id);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();

            // 先读外壳拿到任务类型，data 保持为 JsonNode，
            // 避免直接反序列化成泛型 UploadTaskDto 时丢失 data 的具体类型（会变成 LinkedHashMap）
            UploadTaskDto<JsonNode> envelope = mapper.readValue(
                    uploadDir.resolve("config").toFile(),
                    new TypeReference<UploadTaskDto<JsonNode>>() {
                    }
            );

            if (envelope.getType() == null) {
                throw new IOException("config 中缺少任务类型 type");
            }

            if (envelope.getType() == UploadTaskType.VIDEO) {
                CreateVideoDto data = mapper.convertValue(envelope.getData(), CreateVideoDto.class);
                if (data == null) {
                    throw new IOException("config 中 data 为空");
                }
                processVideo(id, uploadDir, data, emitterId);
            } else {
                log.warn("未知的上传任务类型: {}，id = {}", envelope.getType(), id);
            }
        } catch (Exception e) {
            // 异步线程中的异常无法被调用方捕获，必须在此记录日志
            log.error("异步处理上传任务失败, id = {}", id, e);
        }
    }

    /**
     * 视频类任务：把分片合并成完整视频文件，存到启用的视频根目录，成功后清理临时目录
     */
    private void processVideo(String id, Path uploadDir, CreateVideoDto data, String emitterId) throws IOException {
        String ext = data.getFileExt() != null ? data.getFileExt() : ".mp4";
        File destFileRoot = new File(resolveEnabledVideoRoot(), id);

        if (!destFileRoot.exists()) {
            destFileRoot.mkdir();
        }

        String mainName = IdUtil.fastSimpleUUID();
        String sourceFileName = mainName + ext;
        File destFile = new File(destFileRoot, sourceFileName);

        FileUtil.mkParentDirs(destFile);

        mergeChunk(destFile, uploadDir.toFile(), emitterId, data.getId());

        File tempFile = destFile;

        Long duration = fFmpegUtils.getDuration(destFile.getAbsolutePath());
        final double duration_ns = duration * TimeUnit.SECONDS.toNanos(1);

//        if (!ext.equalsIgnoreCase(".mp4")) {
//            tempFile = new File(destFileRoot, mainName + ".mp4");
//            fFmpegUtils.conversionToMP4(destFile.getAbsolutePath(), tempFile.getAbsolutePath(), progress -> {
//                double percentage = progress.out_time_ns / duration_ns;
//                sendProgress(emitterId, id, "transcoding", percentage);
//            });
//
//        }
//
//        File tsFile = new File(destFileRoot, mainName + ".ts");
//        fFmpegUtils.conversionToTs(tempFile.getAbsolutePath(), tsFile.getAbsolutePath(), progress -> {
//            double percentage = progress.out_time_ns / duration_ns;
//            sendProgress(emitterId, id, "conversion", percentage);
//        });
//
//
//        File m3u8File = new File(destFileRoot, mainName + ".m3u8");
//        fFmpegUtils.conversionToM38u(tsFile.getAbsolutePath(), m3u8File.getAbsolutePath(), progress -> {
//            double percentage = progress.out_time_ns / duration_ns;
//            sendProgress(emitterId, id, "conversion", percentage);
//        });
//
//        FileUtil.del(tsFile);
//        FileUtil.del(destFile);
//        if (FileUtil.exist(tempFile)) {
//            FileUtil.del(tempFile);
//        }
//        // 合并成功后再清理临时目录（失败时保留现场便于排查）
//        FileUtil.del(uploadDir.toFile());
//
//
//        // TODO 后续：等 Video 实体就绪后，把视频记录落库（标题/描述/演员/类型/标签等）
////        videoService.createVideo(data, duration, m3u8File);
//        sendProgress(emitterId, id, "finish", 100);
    }

    /**
     * 按分片编号升序把 chunk_1..chunk_N 合并成完整文件
     */
    private void mergeChunk(File destFile, File uploadDir, String emitterId, String taskId) throws IOException {
        List<File> chunks;
        try (var stream = Files.list(uploadDir.toPath())) {
            chunks = stream.map(Path::toFile)
                    .filter(f -> f.getName().matches("chunk_\\d+"))
                    .sorted(Comparator.comparingInt(f -> parseChunkIndex(f.getName())))
                    .toList();
        }

        if (chunks.isEmpty()) {
            throw new IOException("上传目录中没有找到任何分片: " + uploadDir.getAbsolutePath());
        }

        // 约定从 chunk_1 开始连续编号，缺口即视为分片丢失
        for (int i = 0; i < chunks.size(); i++) {
            if (parseChunkIndex(chunks.get(i).getName()) != i + 1) {
                throw new IOException("分片缺失: 缺少 chunk_" + (i + 1));
            }
        }

        // 按字节计算进度：汇总所有分片大小作为分母
        long totalBytes = 0;
        for (File chunk : chunks) {
            totalBytes += chunk.length();
        }
        if (totalBytes == 0) {
            throw new IOException("分片文件大小均为 0，无法合并: " + uploadDir.getAbsolutePath());
        }

        sendProgress(emitterId, taskId, "merge", 0);

        long mergedBytes = 0;
        int lastRate = -1; // 仅在百分比变化时上报，最多 101 条
        byte[] buffer = new byte[8192];
        try (OutputStream out = Files.newOutputStream(destFile.toPath())) {
            for (File chunk : chunks) {
                try (InputStream in = Files.newInputStream(chunk.toPath())) {
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        out.write(buffer, 0, n);
                        mergedBytes += n;
                        int rate = (int) (mergedBytes * 100 / totalBytes);
                        if (rate != lastRate) {
                            lastRate = rate;
                            sendProgress(emitterId, taskId, "merge", rate);
                        }
                    }
                }
            }
        }
        log.info("合并 {} 个分片: {}", chunks.size(), destFile.getName());
    }

    /**
     * 通过 SSE 上报合并进度。SendMessage 内部已捕获 IOException 并移除失效连接，
     * 因此上报失败不会中断合并流程。
     */
    private void sendProgress(String emitterId, String id, String status, int rate) {
        sseService.SendMessage(emitterId, new SseMessageDto(SseMessageEvent.UPLOAD_STATUS, new HashMap<String, Object>() {{
            put("status", status);
            put("rate", Math.min(100, rate));
            put("id", id);
        }}));
    }

    private void sendProgress(String emitterId, String id, String status, double rate) {
        sseService.SendMessage(emitterId, new SseMessageDto(SseMessageEvent.UPLOAD_STATUS, new HashMap<String, Object>() {{
            put("status", status);
            put("rate", Math.min(1, rate));
            put("id", id);
        }}));
    }

    private int parseChunkIndex(String name) {
        return Integer.parseInt(name.substring("chunk_".length()));
    }

    private String resolveEnabledVideoRoot() {
        String enable = staticResourceDirectoryProperties.getEnableVideoRoot();
        return staticResourceDirectoryProperties.getVideoRoots().stream()
                .filter(root -> enable.equals(root.getName()))
                .findFirst()
                .map(StaticResourceDirectoryProperties.VideoRootProperties::getPath)
                .orElseThrow(() -> new IllegalStateException("未找到启用的视频根目录: " + enable));
    }
}