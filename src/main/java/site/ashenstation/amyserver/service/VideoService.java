package site.ashenstation.amyserver.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.ashenstation.amyserver.config.exception.BadRequestException;
import site.ashenstation.amyserver.dto.CreateVideoDto;
import site.ashenstation.amyserver.dto.UploadTaskDto;
import site.ashenstation.amyserver.entity.*;
import site.ashenstation.amyserver.enums.UploadTaskType;
import site.ashenstation.amyserver.mapper.*;
import site.ashenstation.amyserver.property.StaticResourceDirectoryProperties;
import site.ashenstation.amyserver.utils.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoTypeMapper videoTypeMapper;
    private final VideoTagMapper videoTagMapper;
    private final VideoPublisherMapper videoPublisherMapper;

    private final VideoMapper videoMapper;
    private final VideoTagMapMapper videoTagMapMapper;
    private final VideoArtistMapMapper videoArtistMapMapper;

    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;


    public List<VideoTag> getTag() {
        return videoTagMapper.selectAll();
    }

    public List<VideoType> GetVideoType() {
        return videoTypeMapper.selectAll();
    }

    public List<VideoPublisher> GetVideoPublisher() {
        return videoPublisherMapper.selectAll();
    }

    public String createVideoUploadTask(CreateVideoDto dto) {

        VideoPublisher publisher = dto.getPublisher();
        if (publisher == null) {
            throw new BadRequestException("publisher is null");
        }
        if (publisher.getId() == null) {
            videoPublisherMapper.insert(publisher);
        }

        List<VideoTag> tag = dto.getTag();
        for (VideoTag videoTag : tag) {
            if (videoTag.getId() == null) {
                videoTagMapper.insert(videoTag);
            }
        }

        MultipartFile poster = dto.getPoster();
        String posterName = IdUtil.simpleUUID() + ".webp";
        File posterFile = new File(staticResourceDirectoryProperties.getPosterDirectory(), posterName);
        dto.setPoster(null);
        dto.setPosterName(posterName);

        String id = IdUtil.simpleUUID();

        dto.setId(id);
        dto.setCreatorId(Integer.parseInt(SecurityUtils.getCurrentUserId()));

        String uploadTempDirectory = staticResourceDirectoryProperties.getUploadTempDirectory();
        File file = new File(uploadTempDirectory, id);

        FileUtil.mkdir(file);

        File config = new File(file, "config");

        ObjectMapper mapper = new ObjectMapper();

        UploadTaskDto<CreateVideoDto> uploadTaskDto = new UploadTaskDto<>();
        uploadTaskDto.setId(dto.getId());
        uploadTaskDto.setType(UploadTaskType.VIDEO);
        uploadTaskDto.setData(dto);


        try {
            Files.write(Paths.get(config.getAbsolutePath()), mapper.writeValueAsString(uploadTaskDto).getBytes(StandardCharsets.UTF_8));
            poster.transferTo(posterFile);
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage());
        }

        return id;
    }

    @Transactional
    public void createVideo(CreateVideoDto dto, Long duration, File videoFile) {

        List<VideoTag> tag = dto.getTag();
        VideoPublisher publisher = dto.getPublisher();
        List<Artist> artist = dto.getArtist();


        Video video = new Video();
        video.setTitle(dto.getTitle());
        video.setDescription(dto.getDescription());
        video.setSerialNumber(dto.getSerialNumber());
        video.setType(dto.getType().getId());
        video.setPosterName(dto.getPosterName());
        video.setPublisherId(publisher.getId());

//        合集
//        video.setSeriesId(publisher.getId());
        video.setDuration(duration);
        video.setFileName(videoFile.getName());
        video.setFilePath(videoFile.getAbsolutePath());
        video.setParentFolderName(videoFile.getParent());
        video.setCreatedAt(new Date());
        video.setCreator(dto.getCreatorId());

        videoMapper.insert(video);

        ArrayList<VideoTagMap> videoTagMaps = new ArrayList<>();
        tag.forEach(videoTag -> {
            VideoTagMap videoTagMap = new VideoTagMap();
            videoTagMap.setVideoId(video.getId());
            videoTagMap.setTagId(videoTag.getId());
            videoTagMaps.add(videoTagMap);
        });

        ArrayList<VideoArtistMap> videoArtistMaps = new ArrayList<>();
        artist.forEach(videoArtist -> {
            VideoArtistMap videoArtistMap = new VideoArtistMap();
            videoArtistMap.setArtistId(videoArtist.getId());
            videoArtistMap.setVideoId(video.getId());
            videoArtistMaps.add(videoArtistMap);
        });

        videoTagMapMapper.insertBatch(videoTagMaps);
        videoArtistMapMapper.insertBatch(videoArtistMaps);
    }
}
