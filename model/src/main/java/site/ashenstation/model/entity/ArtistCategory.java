package site.ashenstation.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
@Table("mda_artist_category")
public class ArtistCategory {
    @Id(keyType = KeyType.Auto)
    private Integer id;
    private String title;
    private Date createdAt;
}
