package site.ashenstation.modules.media.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.ashenstation.exception.BadRequestException;
import site.ashenstation.infrastructure.dao.AlbumMapper;
import site.ashenstation.infrastructure.property.StaticResourceDirectoryProperties;
import site.ashenstation.model.entity.Album;
import site.ashenstation.model.entity.table.AlbumTableDef;
import site.ashenstation.modules.media.dto.CreateAlbumDto;
import site.ashenstation.utils.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumMapper albumMapper;
    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;

    public void createAlbum(CreateAlbumDto dto) {

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("相册名不能为空");
        }

        Long artistId;
        try {
            artistId = Long.parseLong(dto.getArtistId());
        } catch (NumberFormatException | NullPointerException e) {
            throw new BadRequestException("艺术家ID不合法");
        }

        // 同一艺术家下相册名不可重复，仅校验正常状态（已删除的相册不占用名称）
        Album exists = albumMapper.selectOneByCondition(AlbumTableDef.ALBUM.NAME.eq(dto.getName())
                .and(AlbumTableDef.ALBUM.ARTIST_ID.eq(artistId))
                .and(AlbumTableDef.ALBUM.STATUS.eq(1)));

        if (exists != null) {
            throw new BadRequestException("该艺术家下已存在同名相册");
        }

        Album album = new Album();
        album.setId(IdUtil.simpleUUID());
        album.setUserId(Long.parseLong(SecurityUtils.getCurrentUserId()));
        album.setArtistId(artistId);
        album.setName(dto.getName());
        album.setDescription(dto.getDescription());
        album.setPhotoCount(0);
        album.setSortOrder(0);
        album.setStatus(1);
        album.setCreatedAt(new Date());
        album.setUpdatedAt(new Date());

        // 封面图复用 poster 目录，cover_photo 仅存文件名
        MultipartFile coverPhoto = dto.getCoverPhoto();
        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            String ext = FileUtil.extName(coverPhoto.getOriginalFilename());
            String coverName = IdUtil.simpleUUID() + (ext == null || ext.isEmpty() ? "" : "." + ext);
            File dest = new File(staticResourceDirectoryProperties.getPosterDirectory(), coverName);
            FileUtil.mkdir(dest.getParentFile());
            try {
                coverPhoto.transferTo(dest);
            } catch (IOException e) {
                throw new BadRequestException("封面文件保存失败");
            }
            album.setCoverPhoto(coverName);
        }

        albumMapper.insert(album);
    }
}
