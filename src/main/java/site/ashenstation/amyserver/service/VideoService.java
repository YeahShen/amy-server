package site.ashenstation.amyserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.ashenstation.amyserver.entity.VideoPublisher;
import site.ashenstation.amyserver.entity.VideoTag;
import site.ashenstation.amyserver.entity.VideoType;
import site.ashenstation.amyserver.mapper.VideoPublisherMapper;
import site.ashenstation.amyserver.mapper.VideoTagMapper;
import site.ashenstation.amyserver.mapper.VideoTypeMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoTypeMapper videoTypeMapper;
    private final VideoTagMapper videoTagMapper;
    private final VideoPublisherMapper videoPublisherMapper;

    public List<VideoTag> getTag() {
        return videoTagMapper.selectAll();
    }

    public List<VideoType> GetVideoType() {
        return videoTypeMapper.selectAll();
    }

    public List<VideoPublisher> GetVideoPublisher() {
        return videoPublisherMapper.selectAll();
    }

    public void createVideoUploadTask() {
    }
}
