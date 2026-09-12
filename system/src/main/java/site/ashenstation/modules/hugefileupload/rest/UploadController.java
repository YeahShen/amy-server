package site.ashenstation.modules.hugefileupload.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.ashenstation.modules.hugefileupload.dto.UploadChunkDto;
import site.ashenstation.modules.hugefileupload.service.UploadService;
import site.ashenstation.modules.hugefileupload.vo.MergeChunkResultVo;

import java.util.ArrayList;
import java.util.HashMap;

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

    @GetMapping("next-step")
    public ResponseEntity<MergeChunkResultVo> nextStep(String id) {
        return ResponseEntity.ok(uploadService.mergeChunk(id));
    }
}
