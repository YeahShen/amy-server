package site.ashenstation.modules.security.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import site.ashenstation.model.entity.AppUser;

@Data
@AllArgsConstructor
public class UserInfoVo {
    private String token;
    private AppUser user;
}
