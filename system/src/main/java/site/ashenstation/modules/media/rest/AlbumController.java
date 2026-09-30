package site.ashenstation.modules.media.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.ashenstation.modules.media.dto.CreateAlbumDto;
import site.ashenstation.modules.media.service.AlbumService;
import site.ashenstation.modules.media.vo.BaseAlbumVo;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/album")
public class AlbumController {

    private final AlbumService albumService;

    /**
     * 创建相册
     * 请求体为 multipart 表单：name、description、artistId、coverPhoto
     * 写操作需登录后携带 JWT 访问
     */
    @PostMapping("/add")
    public ResponseEntity<Void> createAlbum(CreateAlbumDto dto) {
        albumService.createAlbum(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * 查询指定艺术家下的相册列表
     */
    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<BaseAlbumVo>> getAlbumByArtist(@PathVariable String artistId) {
        return ResponseEntity.ok(albumService.getAlbumByArtist(artistId));
    }
}