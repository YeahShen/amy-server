package site.ashenstation.modules.media.vo;

import lombok.Data;
import lombok.ToString;
import site.ashenstation.model.entity.ArtistCategory;

import java.util.Date;

@Data
@ToString
public class ArtistVo {
    private Integer id;
    private String name;
    private String description;
    private String avatar;
    private Date createdAt;
    private ArtistCategory category;
}
