package site.ashenstation.amyserver.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "login")
public class LoginProperties {
    /**
     * 账号单用户 登录
     */
    private boolean singleLogin;

    private long userCacheIdleTime;

    public static final String cacheKey = "user_login_cache:";
}
