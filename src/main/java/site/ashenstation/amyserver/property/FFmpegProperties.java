package site.ashenstation.amyserver.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ffmpeg")
public class FFmpegProperties {
    private String ffmpegExecutorPath;
    private String ffprobeExecutorPath;

    private String conversionToMp4Args;
    private String conversionToTsArgs;

    private String videoEncoder;
    private String audioEncoder;
}

