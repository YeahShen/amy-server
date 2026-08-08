package site.ashenstation.amyserver.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import site.ashenstation.amyserver.entity.User;

@Data
@ToString
@AllArgsConstructor
public class AuthResVo {
    private String token;
    private User user;
}
