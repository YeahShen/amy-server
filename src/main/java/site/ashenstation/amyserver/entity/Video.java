package site.ashenstation.amyserver.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;
import site.ashenstation.amyserver.enums.VideoStatus;

import java.util.Date;

@Table("mda_video")
@Data
@ToString
public class Video {
    @Id
    private String id;
    private String title;
    private String description;
    private String serialNumber;
    private Integer type;
    private String posterName;
    private Integer publisherId;
    private Integer seriesId;
    private Long duration;
    private String parentFolderName;
    private Date createdAt;
    private Integer creator;

    private VideoStatus status;
}
