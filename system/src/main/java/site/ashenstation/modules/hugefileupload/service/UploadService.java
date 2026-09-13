package site.ashenstation.modules.hugefileupload.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import site.ashenstation.enums.UploadTaskType;
import site.ashenstation.exception.BadRequestException;
import site.ashenstation.infrastructure.property.StaticResourceDirectoryProperties;
import site.ashenstation.modules.hugefileupload.dto.CheckChunkDto;
import site.ashenstation.modules.hugefileupload.dto.UploadChunkDto;
import site.ashenstation.modules.hugefileupload.vo.LostChunkVo;
import site.ashenstation.modules.hugefileupload.vo.MergeChunkResultVo;
import site.ashenstation.modules.media.dto.CreateVideoDto;
import site.ashenstation.modules.media.dto.UploadProcessorKeyDto;
import site.ashenstation.modules.media.dto.UploadTaskDto;
import site.ashenstation.utils.FileUtils;
import site.ashenstation.utils.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;
    private final RabbitTemplate rabbitTemplate;

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

    public LostChunkVo checkChunk(CheckChunkDto dto) {
        Path uploadDir = Paths.get(staticResourceDirectoryProperties.getUploadTempDirectory(), dto.getTaskId());

        ArrayList<Integer> loseChunk = new ArrayList<>();

        try {
            var stream = Files.list(uploadDir.toFile().toPath());

            List<File> chunks = stream.map(Path::toFile)
                    .filter(f -> f.getName().matches("chunk_\\d+"))
                    .sorted(Comparator.comparingInt(f -> parseChunkIndex(f.getName())))
                    .toList();

            for (int i = 0; i < chunks.size(); i++) {
                if (parseChunkIndex(chunks.get(i).getName()) != i + 1) {
                    loseChunk.add(i + 1);
                }
            }

            return new LostChunkVo(dto.getTaskId(), loseChunk);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private int parseChunkIndex(String name) {
        return Integer.parseInt(name.substring("chunk_".length()));
    }


    public MergeChunkResultVo mergeChunk(String id) {
        Path uploadDir = Paths.get(staticResourceDirectoryProperties.getUploadTempDirectory(), id);
        ObjectMapper mapper = new ObjectMapper();

        try {
            UploadTaskDto<JsonNode> envelope = mapper.readValue(
                    uploadDir.resolve("config").toFile(),
                    new TypeReference<UploadTaskDto<JsonNode>>() {
                    }
            );

            if (envelope.getType() == null) {
                throw new IOException("config 中缺少任务类型 type");
            }

            List<File> chunks;
            try (var stream = Files.list(uploadDir.toFile().toPath())) {
                chunks = stream.map(Path::toFile)
                        .filter(f -> f.getName().matches("chunk_\\d+"))
                        .sorted(Comparator.comparingInt(f -> parseChunkIndex(f.getName())))
                        .toList();
            }


            if (envelope.getType() == UploadTaskType.VIDEO) {
                CreateVideoDto data = mapper.convertValue(envelope.getData(), CreateVideoDto.class);

                String fileExt = data.getFileExt();
                String fileName = "_" + fileExt + ".temp";

                File tempFileDir = new File(staticResourceDirectoryProperties.getVideoTempDirectory(), data.getId());
                FileUtils.mkdir(tempFileDir);

                File destFile = new File(tempFileDir, fileName);
                FileUtils.mergeFileChunk(destFile, chunks);

                UploadProcessorKeyDto keyDto = new UploadProcessorKeyDto();

                BeanUtils.copyProperties(data, keyDto);
                keyDto.setTaskId(id);
                keyDto.setUserId(SecurityUtils.getCurrentUserId());

                rabbitTemplate.convertAndSend("business.exchange", "video.conversion", mapper.writeValueAsString(keyDto));

                return new MergeChunkResultVo(id, "conversion");
            }


            return new MergeChunkResultVo();
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
