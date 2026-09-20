package site.ashenstation.modules.media.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.ashenstation.model.entity.VideoPublisher;
import site.ashenstation.model.entity.VideoTag;
import site.ashenstation.model.entity.VideoType;
import site.ashenstation.modules.media.dto.CreateVideoDto;
import site.ashenstation.modules.media.service.VideoService;
import site.ashenstation.modules.security.vo.ArtistVideosVo;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final VideoService videoService;


    @GetMapping("get-types")
    public ResponseEntity<List<VideoType>> getAllVideoTypes() {
        return ResponseEntity.ok(videoService.GetVideoType());
    }


    @GetMapping("get-tags")
    public ResponseEntity<List<VideoTag>> getAllVideoTags() {
        return ResponseEntity.ok(videoService.getTag());
    }

    @GetMapping("get-publisher")
    public ResponseEntity<List<VideoPublisher>> getAllVideoPublishers() {
        return ResponseEntity.ok(videoService.GetVideoPublisher());
    }

    @PostMapping("createUploadTask")
    public ResponseEntity<String> createVideoUploadTask(CreateVideoDto dto) {
        return ResponseEntity.ok(videoService.createVideoUploadTask(dto));
    }

    @GetMapping("get-artiest-videos")
    public ResponseEntity<ArtistVideosVo> getVideoListByArtistId(Integer id) {
        return ResponseEntity.ok(videoService.getVideoListByArtistId(id));
    }
}
