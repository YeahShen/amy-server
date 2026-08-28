package site.ashenstation.amyserver.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Table("mda_video_publisher")
public class VideoPublisher {
    @Id(keyType = KeyType.Auto)
    private Integer id;
    private String name;
}
