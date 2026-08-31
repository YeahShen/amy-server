package site.ashenstation.amyserver.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.ashenstation.amyserver.config.exception.BadRequestException;
import site.ashenstation.amyserver.dto.CreateVideoDto;
import site.ashenstation.amyserver.dto.UploadTaskDto;
import site.ashenstation.amyserver.entity.VideoPublisher;
import site.ashenstation.amyserver.entity.VideoTag;
import site.ashenstation.amyserver.entity.VideoType;
import site.ashenstation.amyserver.enums.UploadTaskType;
import site.ashenstation.amyserver.mapper.*;
import site.ashenstation.amyserver.property.StaticResourceDirectoryProperties;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public void createVideo(CreateVideoDto dto, String videoFileName) {

    }
}
