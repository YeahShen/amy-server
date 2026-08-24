package site.ashenstation.amyserver.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "static-resource-directory-properties")
public class StaticResourceDirectoryProperties {
    private String userAvatarDirectory;
    private String artistAvatarDirectory;

    private String userAvatarPathPrefix;
    private String artistAvatarPathPrefix;
}
