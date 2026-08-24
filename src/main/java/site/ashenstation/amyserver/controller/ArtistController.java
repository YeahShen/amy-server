package site.ashenstation.amyserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.ashenstation.amyserver.dto.ArtistDto;
import site.ashenstation.amyserver.entity.Artist;
import site.ashenstation.amyserver.entity.ArtistCategory;
import site.ashenstation.amyserver.service.ArtistService;
import site.ashenstation.amyserver.vo.ArtistByCategoryVo;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/artist")
public class ArtistController {

    private final ArtistService artistService;

    /**
     * 创建艺术家
     * 请求体为 multipart 表单：name、description、avatarFile、category.id / category.title
     * 写操作需登录后携带 JWT 访问
     */
    @PostMapping("/add")
    public ResponseEntity<Boolean> createArtist(@Valid ArtistDto dto) {
        return ResponseEntity.ok(artistService.createArtist(dto));
    }

    /**
     * 根据 ID 查询艺术家详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Artist> getArtistById(@PathVariable Integer id) {
        return ResponseEntity.ok(artistService.getArtistById(id));
    }

    /**
     * 查询艺术家列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<ArtistByCategoryVo>> getArtistList() {
        return ResponseEntity.ok(artistService.getArtistList());
    }

    /**
     * 查询艺术家分类列表
     */
    @GetMapping("/category/list")
    public ResponseEntity<List<ArtistCategory>> getArtistCategoryList() {
        return ResponseEntity.ok(artistService.getArtistCategoryList());
    }
}