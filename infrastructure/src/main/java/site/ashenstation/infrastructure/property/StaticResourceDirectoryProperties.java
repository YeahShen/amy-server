package site.ashenstation.infrastructure.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "static-resource-directory-properties")
public class StaticResourceDirectoryProperties {
    private String userAvatarDirectory;
    private String artistAvatarDirectory;

    private String userAvatarPathPrefix;
    private String artistAvatarPathPrefix;

    private String posterDirectory;
    private String posterPathPrefix;

    private List<VideoRootProperties> videoRoots;
    private String videoResourcePrefix;

    private String enableVideoRoot;

    private String uploadTempDirectory;

    private String videoTempDirectory;


    @Data
    public static class VideoRootProperties {
        private String name;
        private String path;
    }
}
