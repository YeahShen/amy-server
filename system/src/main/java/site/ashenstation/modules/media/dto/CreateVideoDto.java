package site.ashenstation.modules.media.dto;

import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import site.ashenstation.model.entity.Artist;
import site.ashenstation.model.entity.VideoPublisher;
import site.ashenstation.model.entity.VideoTag;
import site.ashenstation.model.entity.VideoType;

import java.util.List;

@Data
@ToString
public class CreateVideoDto {
    private String id;
    private String title;
    private String description;
    private String serialNumber;
    private VideoType type;
    private MultipartFile poster;
    private String posterName;
    private String fileExt;
    private List<Artist> artist;
    private VideoPublisher publisher;
    private List<VideoTag> tag;
    private Integer creatorId;
    private Integer seriesId;
}
