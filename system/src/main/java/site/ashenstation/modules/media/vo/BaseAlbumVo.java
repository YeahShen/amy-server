package site.ashenstation.modules.media.vo;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class BaseAlbumVo {
    private String id;
    private String name;
    private String description;
    private String coverPhoto;
    private Integer photoCount;
    private Integer sortOrder;
    private Date createdAt;
    private Date updatedAt;
}
