package site.ashenstation.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
@Table("sys_aes")
public class ResourceAesEncrypt {
    @Id(keyType = KeyType.Auto)
    private Integer id;
    private String resourceId;
    private String encryptKey;
    private Date createAt;
}
