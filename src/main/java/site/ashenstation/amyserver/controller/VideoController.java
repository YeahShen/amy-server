package site.ashenstation.amyserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.ashenstation.amyserver.entity.VideoPublisher;
import site.ashenstation.amyserver.entity.VideoTag;
import site.ashenstation.amyserver.entity.VideoType;
import site.ashenstation.amyserver.service.VideoService;

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
}
