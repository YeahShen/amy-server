package site.ashenstation.amyserver.service;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.util.UpdateEntity;
import freemarker.template.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import site.ashenstation.amyserver.config.exception.BadRequestException;
import site.ashenstation.amyserver.dto.CreateVideoDto;
import site.ashenstation.amyserver.dto.UploadProcessorKeyDto;
import site.ashenstation.amyserver.dto.UploadTaskDto;
import site.ashenstation.amyserver.dto.VideoTemporaryInformationDto;
import site.ashenstation.amyserver.entity.*;
import site.ashenstation.amyserver.enums.UploadTaskType;
import site.ashenstation.amyserver.enums.VideoStatus;
import site.ashenstation.amyserver.mapper.*;
import site.ashenstation.amyserver.property.FFmpegProperties;
import site.ashenstation.amyserver.property.StaticResourceDirectoryProperties;
import site.ashenstation.amyserver.utils.FFmpegUtils;
import site.ashenstation.amyserver.utils.FileUtils;
import site.ashenstation.amyserver.utils.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoTypeMapper videoTypeMapper;
    private final VideoTagMapper videoTagMapper;
    private final VideoPublisherMapper videoPublisherMapper;

    private final VideoMapper videoMapper;
    private final VideoTagMapMapper videoTagMapMapper;
    private final VideoArtistMapMapper videoArtistMapMapper;
    private final FFmpegUtils fFmpegUtils;
    private final FFmpegProperties fFmpegProperties;
    private final Configuration freemarkerConfig;

    private final TransactionTemplate transactionTemplate;

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

        FileUtils.mkdir(file);

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


    public void processVideoUploadNext(UploadProcessorKeyDto uploadProcessorKeyDto, CreateVideoDto data) {
        Path uploadDir = Paths.get(staticResourceDirectoryProperties.getUploadTempDirectory(), uploadProcessorKeyDto.getTaskId());

        try {

            String ext = data.getFileExt() != null ? data.getFileExt() : ".mp4";
            String fileMainName = IdUtil.fastSimpleUUID();

            String fileName = fileMainName + ext;
            File destRootFile = new File(resolveEnabledVideoRoot(), uploadProcessorKeyDto.getTaskId());

            File destFile = new File(destRootFile, fileName);

            FileUtils.mkParentDirs(destFile);

            List<File> chunks;
            try (var stream = Files.list(uploadDir.toFile().toPath())) {
                chunks = stream.map(Path::toFile)
                        .filter(f -> f.getName().matches("chunk_\\d+"))
                        .sorted(Comparator.comparingInt(f -> parseChunkIndex(f.getName())))
                        .toList();
            }

            if (chunks.isEmpty()) {
                throw new IOException("上传目录中没有找到任何分片: " + uploadDir.toFile().getAbsolutePath());
            }

            for (int i = 0; i < chunks.size(); i++) {
                if (parseChunkIndex(chunks.get(i).getName()) != i + 1) {
                    throw new IOException("分片缺失: 缺少 chunk_" + (i + 1));
                }
            }

            FileUtils.mergeFileChunk(destFile, chunks);

            Long duration = fFmpegUtils.getDuration(destFile.getAbsolutePath());

            VideoTemporaryInformationDto videoTemporaryInformationDto = saveVideoInformation(data, duration, destFile);

//            insert
            videoTagMapMapper.insertBatch(videoTemporaryInformationDto.getVideoTagMaps());
            videoArtistMapMapper.insertBatch(videoTemporaryInformationDto.getVideoArtistMaps());
            videoMapper.insert(videoTemporaryInformationDto.getVideo());

            FFmpegExecutor fFmpegExecutor = fFmpegUtils.getExecutor(destRootFile);

            File mp4File;
            if (!ext.equals(".mp4")) {
                mp4File = new File(destRootFile, fileMainName + ".mp4");
                fFmpegUtils.conversion(destFile, mp4File, fFmpegProperties.getConversionToMp4Args(), fFmpegExecutor);
            } else {
                mp4File = destFile;
            }

            File tsFile = new File(destRootFile, fileMainName + ".ts");
            fFmpegUtils.conversion(mp4File, tsFile, fFmpegProperties.getConversionToTsArgs(), fFmpegExecutor);

            FFmpegUtils.VideoResolution videoResolution = fFmpegUtils.getVideoResolution(destFile.getAbsolutePath());

            FFmpegUtils.ConversionPlan conversionPlan = fFmpegUtils.generateAdaptiveFFmpegCommand(videoResolution.width(), videoResolution.height(), fFmpegProperties.getVideoEncoder(), fFmpegProperties.getAudioEncoder());

            List<String> label = conversionPlan.label();

            // 预创建各清晰度目录：目录名须与 -var_stream_map 的 name（v4k/v2k/...）一致，%v 会被替换成该名字
            for (String type : label) {
                FileUtils.mkdir(new File(destRootFile, type));
            }

            fFmpegUtils.conversion(tsFile.getAbsolutePath(), "%v/index.m3u8", conversionPlan.command(), fFmpegExecutor, new ProgressListener() {
                final double duration_ns = duration * TimeUnit.SECONDS.toNanos(1);

                @Override
                public void progress(Progress progress) {
                    double percentage = progress.out_time_ns / duration_ns;
                    System.out.println(percentage);
                }
            });

            Video video1 = UpdateEntity.of(Video.class, data.getId());
            video1.setStatus(VideoStatus.NORMAL);

            videoMapper.update(video1);

//            删除临时文件
            FileUtils.del(destFile);

            if (FileUtils.exist(mp4File)) {
                FileUtils.del(mp4File);
            }

            FileUtils.del(tsFile);


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        File uploadDirFile = uploadDir.toFile();
        FileUtils.del(uploadDirFile);

    }


    public VideoTemporaryInformationDto saveVideoInformation(CreateVideoDto dto, Long duration, File destFile) {
        List<VideoTag> tag = dto.getTag();
        VideoPublisher publisher = dto.getPublisher();
        List<Artist> artist = dto.getArtist();

        Video video = new Video();
        video.setId(dto.getId());
        video.setTitle(dto.getTitle());
        video.setDescription(dto.getDescription());
        video.setSerialNumber(dto.getSerialNumber());
        video.setType(dto.getType().getId());
        video.setPosterName(dto.getPosterName());
        video.setPublisherId(publisher.getId());

        video.setSeriesId(dto.getSeriesId());
        video.setDuration(duration);
        video.setParentFolderName(destFile.getParent());
        video.setCreatedAt(new Date());
        video.setCreator(dto.getCreatorId());
        video.setStatus(VideoStatus.CONVERSION);

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

        return new VideoTemporaryInformationDto(videoArtistMaps, videoTagMaps, video);
    }

    private String resolveEnabledVideoRoot() {
        String enable = staticResourceDirectoryProperties.getEnableVideoRoot();
        return staticResourceDirectoryProperties.getVideoRoots().stream()
                .filter(root -> enable.equals(root.getName()))
                .findFirst()
                .map(StaticResourceDirectoryProperties.VideoRootProperties::getPath)
                .orElseThrow(() -> new IllegalStateException("未找到启用的视频根目录: " + enable));
    }

    private int parseChunkIndex(String name) {
        return Integer.parseInt(name.substring("chunk_".length()));
    }

}
