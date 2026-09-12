package site.ashenstation.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;

@Table("mda_video_artist_map")
@Data
@ToString
public class VideoArtistMap {
    @Id(keyType = KeyType.Auto)
    private Integer id;
    private String videoId;
    private Integer artistId;
}
