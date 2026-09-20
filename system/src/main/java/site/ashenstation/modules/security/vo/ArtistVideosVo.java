package site.ashenstation.modules.security.vo;

import com.mybatisflex.annotation.TableRef;
import lombok.Data;
import lombok.ToString;
import site.ashenstation.model.entity.Video;

import java.util.List;

@Data
@ToString
@TableRef(Video.class)
public class ArtistVideosVo {
    private String artistId;
    private List<VideoVo> videoList;
}
