package site.ashenstation.amyserver.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;

@Table("mda_video_tag_map")
@Data
@ToString
public class VideoTagMap {
    @Id(keyType = KeyType.Auto)
    private Integer id;
    private Integer videoId;
    private Integer tagId;
}
