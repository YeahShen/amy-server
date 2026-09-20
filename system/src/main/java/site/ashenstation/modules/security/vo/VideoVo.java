package site.ashenstation.modules.security.vo;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class VideoVo {
    private String id;
    private String title;
    private String description;
    private String serialNumber;
    private String typeTitle;
    private String posterName;
    private String posterUrl;
    private Integer publisherId;
    private Integer seriesId;
    private Long duration;
    private String parentFolderName;
    private Date createdAt;
    private String resolutions;

}
