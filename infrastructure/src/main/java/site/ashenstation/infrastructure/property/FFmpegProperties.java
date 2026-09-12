package site.ashenstation.infrastructure.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "ffmpeg")
public class FFmpegProperties {
    private String ffmpegExecutorPath;
    private String ffprobeExecutorPath;

    private String videoEncoder;
    private String audioEncoder;

    /**
     * 硬件设备参数（空格分隔，空字符串表示纯软编）。
     * Intel QSV：-init_hw_device qsv=hw -filter_hw_device hw（帧以 nv12 上传，无需 hwupload）
     * AMD AMF： -init_hw_device d3d11va=hw -filter_hw_device hw（滤镜链内加 format=nv12,hwupload）
     */
    private String hwArgs;
    private String videoEncoderArgs;
    private String audioEncoderArgs;

    private List<String> doNotReEncodingFormat;
}