package site.ashenstation.amyserver.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UploadProcessorKeyDto {
    private Integer userId;
    private String tokenUid;
    private String taskId;
}
