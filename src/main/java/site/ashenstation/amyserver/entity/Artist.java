package site.ashenstation.amyserver.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
@Table("mda_artist")
public class Artist {
    @Id(keyType = KeyType.Auto)
    private Integer id;
    private String name;
    private String description;
    private String avatar;
    private Date createdAt;
    private Integer categoryId;
}
