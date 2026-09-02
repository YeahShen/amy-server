package site.ashenstation.amyserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.ashenstation.amyserver.annotation.UploadProcess;
import site.ashenstation.amyserver.config.exception.BadRequestException;
import site.ashenstation.amyserver.dto.UploadChunkDto;
import site.ashenstation.amyserver.property.StaticResourceDirectoryProperties;
import site.ashenstation.amyserver.utils.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;
    private final UploadProcessService uploadProcessService;

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

    @UploadProcess
    public String nextStep(String id) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        String tokenUid = SecurityUtils.getTokenUid();

        return id;
        // 委托给独立的 @Async bean 调用，经 Spring 代理后异步执行
//        uploadProcessService.nextStepDeal(id, currentUserId + ":" + tokenUid);
    }
}
