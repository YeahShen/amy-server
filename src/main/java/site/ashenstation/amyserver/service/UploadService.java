package site.ashenstation.amyserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import site.ashenstation.amyserver.config.exception.BadRequestException;
import site.ashenstation.amyserver.dto.CreateVideoDto;
import site.ashenstation.amyserver.dto.UploadChunkDto;
import site.ashenstation.amyserver.dto.UploadProcessorKeyDto;
import site.ashenstation.amyserver.dto.UploadTaskDto;
import site.ashenstation.amyserver.enums.UploadTaskType;
import site.ashenstation.amyserver.property.StaticResourceDirectoryProperties;
import site.ashenstation.amyserver.utils.RedisUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;
    private final VideoService videoService;
    private final RedisUtils redisUtils;

    public HashMap<String, Object> uploadChunk(UploadChunkDto dto) {
        String id = dto.getId();

        String uploadTempDirectory = staticResourceDirectoryProperties.getUploadTempDirectory();
        File file = new File(uploadTempDirectory, id);

        File chunk = new File(file, "chunk_" + dto.getIndex().toString());

        try {
            dto.getChunk().transferTo(chunk);
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage());
        }

        return new HashMap<>() {{
            put("status", "success");
            put("index", dto.getIndex());
        }};
    }

    @Async("AmyTaskExecutor")
    public void nextStep(UploadProcessorKeyDto uploadProcessorKeyDto) {
        try {

            Path uploadDir = Paths.get(staticResourceDirectoryProperties.getUploadTempDirectory(), uploadProcessorKeyDto.getTaskId());


            // 任务被处理过（临时目录已清理）则直接跳过，保证接口可重复调用
            if (!Files.exists(uploadDir)) {
                log.warn("上传任务 {} 不存在或已处理完毕", uploadProcessorKeyDto.getTaskId());
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

                redisUtils.hset("upload-task:" + envelope.getType(), uploadProcessorKeyDto.getTaskId(), uploadProcessorKeyDto);

                videoService.processVideoUploadNext(uploadProcessorKeyDto, data);

            } else {
                log.warn("未知的上传任务类型: {}，id = {}", envelope.getType(), uploadProcessorKeyDto.getTaskId());
            }

        } catch (Exception e) {
            // 异步线程中的异常无法被调用方捕获，必须在此记录日志
            log.error("异步处理上传任务失败, id = {}", uploadProcessorKeyDto.getTaskId(), e);
        }
    }
}
