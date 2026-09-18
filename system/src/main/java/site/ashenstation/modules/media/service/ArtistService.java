package site.ashenstation.modules.media.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.query.QueryWrapper;
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
import site.ashenstation.modules.media.vo.ArtistVo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        // 单表查询后在内存分组：多表 join 的嵌套映射依赖 <表名>$<列名> 这种自动别名，列重名时映射不上（Artist.id 曾因此恒为 null）
        Map<Integer, List<Artist>> artistMap = artistMapper.selectAll().stream()
                .filter(artist -> artist.getCategoryId() != null)
                .collect(Collectors.groupingBy(Artist::getCategoryId));

        return artistCategoryMapper.selectAll().stream()
                .map(category -> {
                    ArtistByCategoryVo vo = new ArtistByCategoryVo();
                    vo.setId(category.getId().toString());
                    vo.setTitle(category.getTitle());
                    vo.setList(artistMap.getOrDefault(category.getId(), new ArrayList<>()));
                    vo.processAvatarUrl(staticResourceDirectoryProperties.getArtistAvatarPathPrefix());
                    return vo;
                })
                .toList();
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


    public ArtistVo getArtistInfo(Integer id) {

        QueryWrapper wrapper = QueryWrapper.create()
                .select(ArtistTableDef.ARTIST.ALL_COLUMNS, ArtistCategoryTableDef.ARTIST_CATEGORY.ALL_COLUMNS)
                .from(ArtistTableDef.ARTIST.as("a")).where(ArtistTableDef.ARTIST.ID.eq(id))
                .leftJoin(ArtistCategoryTableDef.ARTIST_CATEGORY).as("c").on(ArtistTableDef.ARTIST.CATEGORY_ID.eq(ArtistCategoryTableDef.ARTIST_CATEGORY.ID));

        ArtistVo artistVo = artistMapper.selectOneByQueryAs(wrapper, ArtistVo.class);

        return artistVo;
    }

}
