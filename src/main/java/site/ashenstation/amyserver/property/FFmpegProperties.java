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
    private String conversionToM3u84kArgs;
    private String conversionToM3u82kArgs;
    private String conversionToM3u81080pArgs;
    private String conversionToM3u8720pArgs;
    private String conversionToM3u8480pArgs;
}

