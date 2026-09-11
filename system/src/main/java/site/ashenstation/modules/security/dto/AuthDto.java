package site.ashenstation.modules.security.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AuthDto {
    private String username;
    private String password;
}
