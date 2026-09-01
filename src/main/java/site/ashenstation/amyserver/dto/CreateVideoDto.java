package site.ashenstation.amyserver.dto;

import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import site.ashenstation.amyserver.entity.Artist;
import site.ashenstation.amyserver.entity.VideoPublisher;
import site.ashenstation.amyserver.entity.VideoTag;
import site.ashenstation.amyserver.entity.VideoType;

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
}
