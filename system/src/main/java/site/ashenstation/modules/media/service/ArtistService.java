package site.ashenstation.modules.media.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.query.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.ashenstation.exception.BadRequestException;
import site.ashenstation.infrastructure.dao.ArtistCategoryMapper;
import site.ashenstation.infrastructure.dao.ArtistMapper;
import site.ashenstation.infrastructure.property.StaticResourceDirectoryProperties;
import site.ashenstation.model.entity.Artist;
import site.ashenstation.model.entity.ArtistCategory;
import site.ashenstation.model.entity.table.ArtistCategoryTableDef;
import site.ashenstation.model.entity.table.ArtistTableDef;
import site.ashenstation.modules.media.dto.ArtistDto;
import site.ashenstation.modules.media.vo.ArtistByCategoryVo;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistCategoryMapper artistCategoryMapper;
    private final ArtistMapper artistMapper;
    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;

    public Boolean createArtist(ArtistDto dto) {
        Artist artist = artistMapper.selectOneByCondition(ArtistTableDef.ARTIST.NAME.eq(dto.getName()));
        if (artist != null) {
            throw new BadRequestException("Artist already exists");
        }

        artist = new Artist();
        artist.setName(dto.getName());

        ArtistCategory category = dto.getCategory();

        if (category == null) {
            throw new BadRequestException("Category can not be null");
        }

        if (category.getId() == null) {
            category.setCreatedAt(new Date());
            artistCategoryMapper.insert(category);
        }

        artist.setCategoryId(category.getId());

        // 生成唯一文件名，并将头像文件保存到静态资源目录
        String avatarName = IdUtil.fastSimpleUUID() + ".png";
        MultipartFile avatarFile = dto.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            File dest = new File(staticResourceDirectoryProperties.getArtistAvatarDirectory(), avatarName);
            FileUtil.mkdir(dest.getParentFile());
            try {
                avatarFile.transferTo(dest);
            } catch (IOException e) {
                throw new BadRequestException("头像文件保存失败");
            }
        }

        artist.setAvatar(avatarName);
        artist.setDescription(dto.getDescription());
        artist.setCreatedAt(new Date());

        artistMapper.insert(artist);

        return true;
    }

    public Artist getArtistById(Integer id) {
        Artist artist = artistMapper.selectOneByCondition(ArtistTableDef.ARTIST.ID.eq(id));

        if (artist == null) {
            throw new BadRequestException("Artist not found");
        }
        artist.setAvatar(staticResourceDirectoryProperties.getArtistAvatarPathPrefix() + "/" + artist.getAvatar());

        return artist;
    }


    public List<ArtistByCategoryVo> getArtistList() {

        List<ArtistByCategoryVo> artistByCategoryVos = QueryChain.of(artistMapper)
                .select(ArtistCategoryTableDef.ARTIST_CATEGORY.ALL_COLUMNS, ArtistTableDef.ARTIST.ALL_COLUMNS)
                .from(ArtistCategoryTableDef.ARTIST_CATEGORY)
                .leftJoin(ArtistTableDef.ARTIST).on(ArtistTableDef.ARTIST.CATEGORY_ID.eq(ArtistCategoryTableDef.ARTIST_CATEGORY.ID))
                .listAs(ArtistByCategoryVo.class);

        artistByCategoryVos.forEach(artist -> {
            artist.processAvatarUrl(staticResourceDirectoryProperties.getArtistAvatarPathPrefix());
        });


        return artistByCategoryVos;
    }


    public List<ArtistCategory> getArtistCategoryList() {
        return artistCategoryMapper.selectAll();
    }


    public List<Artist> getAllArtists() {
        List<Artist> artists = artistMapper.selectAll();

        assert artists != null;
        artists.forEach(artist -> {
            artist.setAvatar(staticResourceDirectoryProperties.getArtistAvatarPathPrefix() + "/" + artist.getAvatar());
        });
        return artists;
    }

}
