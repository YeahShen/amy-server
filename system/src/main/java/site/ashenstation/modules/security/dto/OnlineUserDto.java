package site.ashenstation.modules.security.dto;

import lombok.Data;
import lombok.ToString;
import site.ashenstation.enums.LoginPlatform;

import java.util.Date;

@Data
@ToString
public class OnlineUserDto {
    private String userName;
    private String ip;
    private String address;
    private String key;
    private Date loginTime;
    private LoginPlatform loginPlatform;
}
