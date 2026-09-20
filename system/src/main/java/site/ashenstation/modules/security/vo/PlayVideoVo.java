package site.ashenstation.modules.security.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PlayVideoVo {
    private String title;
    private String description;
    private Long duration;
    private String playUrl;
    private String decryptKey;
}
