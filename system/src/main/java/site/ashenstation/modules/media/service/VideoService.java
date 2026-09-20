package site.ashenstation.modules.media.service;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.ashenstation.enums.UploadTaskType;
import site.ashenstation.enums.VideoStatus;
import site.ashenstation.exception.BadRequestException;
import site.ashenstation.infrastructure.dao.*;
import site.ashenstation.infrastructure.property.FFmpegProperties;
import site.ashenstation.infrastructure.property.StaticResourceDirectoryProperties;
import site.ashenstation.model.entity.*;
import site.ashenstation.model.entity.table.VideoArtistMapTableDef;
import site.ashenstation.model.entity.table.VideoTableDef;
import site.ashenstation.model.entity.table.VideoTypeTableDef;
import site.ashenstation.modules.media.dto.CreateVideoDto;
import site.ashenstation.modules.media.dto.UploadProcessorKeyDto;
import site.ashenstation.modules.media.dto.UploadTaskDto;
import site.ashenstation.modules.security.service.SseService;
import site.ashenstation.modules.security.vo.ArtistVideosVo;
import site.ashenstation.modules.security.vo.NotificationVO;
import site.ashenstation.utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoTypeMapper videoTypeMapper;
    private final VideoTagMapper videoTagMapper;
    private final VideoPublisherMapper videoPublisherMapper;

    private final VideoMapper videoMapper;
    private final VideoTagMapMapper videoTagMapMapper;
    private final VideoArtistMapMapper videoArtistMapMapper;
    private final FFmpegProperties fFmpegProperties;
    private final TemplateRenderUtils templateRenderUtils;
    private final FFmpegUtils fFmpegUtils;
    private final SseService sseService;
    private final VideoConversionService videoConversionService;
    private final ResourceAesEncryptMapper resourceAesEncryptMapper;

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

    @RabbitListener(queues = "video.conversion")
    public void finishVideoUploadTask(String message, Channel channel) throws IOException {
        try {

            ObjectMapper mapper = new ObjectMapper();
            UploadProcessorKeyDto dto = mapper.readValue(message, UploadProcessorKeyDto.class);

            String fileExt = dto.getFileExt();

            File tempFile = new File(staticResourceDirectoryProperties.getVideoTempDirectory(), dto.getId() + "/" + "_" + fileExt + ".temp");
            File destFile = new File(staticResourceDirectoryProperties.getVideoTempDirectory(), dto.getId() + "/" + "_" + fileExt);

            tempFile.renameTo(destFile);

            Long duration = fFmpegUtils.getDuration(destFile.getAbsolutePath());

            saveVideoInformation(dto, duration);

            String userId = dto.getUserId();
            sseService.SendMessage(userId, new NotificationVO("reload", new HashMap<>()));

            String encodingFormat = fFmpegUtils.getEncodingFormat(destFile.getAbsolutePath());
            File videoFile = destFile;

            FFmpegExecutor executor = fFmpegUtils.getExecutor(destFile.getParentFile());

            final double duration_ns = duration * TimeUnit.SECONDS.toNanos(1);

            if (!fFmpegProperties.getDoNotReEncodingFormat().contains(encodingFormat)) {
                //                需要转码
                videoFile = new File(destFile.getParentFile(), "__.mp4");
                String toH265 = templateRenderUtils.render("ffmpeg/to_h265.ftlh", new HashMap<>() {{
                    put("videoEncoder", fFmpegProperties.getVideoEncoder());
                }});

                fFmpegUtils.conversion(destFile.getAbsolutePath(), videoFile.getAbsolutePath(), toH265, executor, new ProgressListener() {

                    @Override
                    public void progress(Progress progress) {
                        double percentage = progress.out_time_ns / duration_ns;
                        log.info("taskId: {}, conversion to h256 percentage: {}", dto.getId(), percentage);
                    }
                });
            }

            String toTsCmd = templateRenderUtils.render("ffmpeg/to_ts.ftlh", new HashMap<>());
            File tsFile = new File(destFile.getParentFile(), "__.ts");
            fFmpegUtils.conversion(videoFile.getAbsolutePath(), tsFile.getAbsolutePath(), toTsCmd, executor, new ProgressListener() {
                @Override
                public void progress(Progress progress) {
                    double percentage = progress.out_time_ns / duration_ns;
                    log.info("taskId: {}, conversion to ts percentage: {}", dto.getId(), percentage);
                }
            });

            videoFile = tsFile;


            System.out.println("encodingFormat: " + encodingFormat);

            FFmpegUtils.VideoResolution videoResolution = fFmpegUtils.getVideoResolution(videoFile.getAbsolutePath());

            List<FFmpegUtils.VideoVariant> videoResolutions = fFmpegUtils.getVideoResolutions(videoResolution.width(), videoResolution.height());

            ArrayList<CompletableFuture<String>> completableFutures = new ArrayList<>();

            File root = new File(resolveEnabledVideoRoot(), dto.getId());
            FileUtils.mkdir(root);

            FFmpegExecutor rootExecutor = fFmpegUtils.getExecutor(root);

            for (FFmpegUtils.VideoVariant variant : videoResolutions) {
                String label = variant.label();
                String cmd = templateRenderUtils.render("ffmpeg/" + label + "_hls.ftlh", new HashMap<>() {{
                    put("videoEncoder", fFmpegProperties.getVideoEncoder());
                    put("audioEncoder", fFmpegProperties.getAudioEncoder());
                    put("hwArgs", fFmpegProperties.getHwArgs());
                    put("videoEncoderArgs", fFmpegProperties.getVideoEncoderArgs());
                    put("audioEncoderArgs", fFmpegProperties.getAudioEncoderArgs());
                }});

                new File(root, label).mkdir();
                completableFutures.add(videoConversionService.convertVideo(rootExecutor, videoFile.getAbsolutePath(), label + "/index.m3u8", cmd));
            }

            CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();


            log.info("taskId: {} 全部档位转码完成: {}", dto.getId(), videoResolutions);

            // 生成 master.m3u8：各档位的标签、分辨率、码率、编码格式汇总
            // 编码串取第一个档位的首个分片实测；探测失败时兜底为最常见的 H.264 High@4.0 + AAC-LC
            String codec;
            try {
                File firstSegment = new File(root, videoResolutions.get(0).label() + "/segment_000.ts");
                codec = fFmpegUtils.getCodecString(firstSegment.getAbsolutePath());
            } catch (IOException e) {
                log.warn("taskId: {} 探测编码格式失败，master.m3u8 使用兜底 CODECS：{}", dto.getId(), e.getMessage());
                codec = null;
            }
            if (codec == null) {
                codec = "avc1.640028,mp4a.40.2";
            }

            final String codecString = codec;

            String masterPlaylist = templateRenderUtils.render("ffmpeg/master.ftlh", new HashMap<>() {{
                put("variants", videoResolutions);
                put("codec", codecString);
            }});
            Files.writeString(new File(root, "master.m3u8").toPath(), masterPlaylist, StandardCharsets.UTF_8);

            Video video = UpdateEntity.of(Video.class, dto.getId());
            video.setStatus(VideoStatus.NORMAL);

            List<String> list = videoResolutions.stream().map(FFmpegUtils.VideoVariant::label).toList();
            video.setResolutions(String.join(",", list));

            videoMapper.update(video);

            FileUtils.del(destFile.getAbsolutePath());

            sseService.SendMessage(userId, new NotificationVO("reloadArtistVideo", new HashMap<>() {{
                put("artist", dto.getId());
            }}));

            String key = AesUtils.generateKey();

            channel.basicAck(1L, false);
            ResourceAesEncrypt resourceAesEncrypt = new ResourceAesEncrypt();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void saveVideoInformation(CreateVideoDto dto, Long duration) {
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
        video.setParentFolderName(dto.getId());
        video.setCreatedAt(new Date());
        video.setCreator(dto.getCreatorId());
        video.setStatus(VideoStatus.CONVERSION);

        File file = new File(resolveEnabledVideoRoot(), dto.getId());
        video.setRootAbsolutePath(file.getAbsolutePath());

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

        videoMapper.insert(video);
        videoArtistMapMapper.insertBatch(videoArtistMaps);
        videoTagMapMapper.insertBatch(videoTagMaps);
    }

    private String resolveEnabledVideoRoot() {
        String enable = staticResourceDirectoryProperties.getEnableVideoRoot();
        return staticResourceDirectoryProperties.getVideoRoots().stream()
                .filter(root -> enable.equals(root.getName()))
                .findFirst()
                .map(StaticResourceDirectoryProperties.VideoRootProperties::getPath)
                .orElseThrow(() -> new IllegalStateException("未找到启用的视频根目录: " + enable));
    }


    public ArtistVideosVo getVideoListByArtistId(Integer artistId) {
        VideoArtistMapTableDef videoArtistMap = VideoArtistMapTableDef.VIDEO_ARTIST_MAP;
        VideoTableDef videoTable = VideoTableDef.VIDEO;

        QueryWrapper wrapper = QueryWrapper.create()
                .select(videoArtistMap.ARTIST_ID, videoArtistMap.VIDEO_ID, videoTable.ALL_COLUMNS, videoTable.TYPE, VideoTypeTableDef.VIDEO_TYPE.ID, VideoTypeTableDef.VIDEO_TYPE.TITLE.as("type_title"))
                .from(videoArtistMap.as("a")).where(videoArtistMap.ARTIST_ID.eq(artistId))
                .leftJoin(videoTable.as("v")).on(videoTable.ID.eq(videoArtistMap.VIDEO_ID))
                .leftJoin(VideoTypeTableDef.VIDEO_TYPE.as("t")).on(videoTable.TYPE.eq(VideoTypeTableDef.VIDEO_TYPE.ID));

        ArtistVideosVo artistVideosVo = videoArtistMapMapper.selectOneByQueryAs(wrapper, ArtistVideosVo.class);

        artistVideosVo.getVideoList().forEach(video -> {
            String posterName = video.getPosterName();
            video.setPosterUrl(staticResourceDirectoryProperties.getPosterPathPrefix() + "/" + posterName);
        });

        return artistVideosVo;
    }
}
