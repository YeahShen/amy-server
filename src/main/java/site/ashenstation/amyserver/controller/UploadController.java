package site.ashenstation.amyserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.ashenstation.amyserver.annotation.UploadProcess;
import site.ashenstation.amyserver.dto.UploadChunkDto;
import site.ashenstation.amyserver.service.UploadService;
import site.ashenstation.amyserver.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("chunk")
    public ResponseEntity<HashMap<String, Object>> uploadChunk(UploadChunkDto dto) {
        return ResponseEntity.ok(uploadService.uploadChunk(dto));
    }

    @GetMapping("check-chunk")
    public ResponseEntity<HashMap<String, Object>> uploadFile(String id) {
        return ResponseEntity.ok(new HashMap<>() {{
            put("loseChunk", new ArrayList<>());
        }});
    }


    @UploadProcess
    @GetMapping("next-step")
    public Map<String, String> nextStep(String id) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        String tokenUid = SecurityUtils.getTokenUid();

        return new HashMap<>() {{
            put("taskId", id);
            put("currentUserId", currentUserId);
            put("tokenUid", tokenUid);
        }};
    }
}
