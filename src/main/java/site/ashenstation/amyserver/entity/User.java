package site.ashenstation.amyserver.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@ToString
@Table("sys_user")
public class User {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Boolean enabled;
    private Boolean accountNonExpired;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    private Date createTime;
    private Date updateTime;
    private Date lastLoginTime;
    private String lastLoginIp;
}
