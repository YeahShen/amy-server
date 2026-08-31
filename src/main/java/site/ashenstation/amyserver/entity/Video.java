package site.ashenstation.amyserver.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Table("mda_video")
@Data
@ToString
public class Video {
    @Id(keyType = KeyType.Auto)
    private Integer id;
    private String title;
    private String description;
    private String serialNumber;
    private String type;
    private String posterName;
    private Integer publisherId;
    private Integer seriesId;
    private Long duration;
    private String filePath;
    private String fileName;
    private Date createdAt;
    private Integer creator;
}
