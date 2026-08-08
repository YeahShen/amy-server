package site.ashenstation.amyserver.dto;

import lombok.Data;
import site.ashenstation.amyserver.enums.LoginPlatform;

import java.util.Date;

@Data
public class OnlineUserDto {
    private String userName;
    private String ip;

    private String address;

    private String key;

    private Date loginTime;

    private LoginPlatform loginPlatform;
}
