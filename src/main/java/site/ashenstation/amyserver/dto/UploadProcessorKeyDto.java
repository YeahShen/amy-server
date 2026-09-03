package site.ashenstation.amyserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class UploadProcessorKeyDto {
    private Integer userId;
    private String tokenUid;
    private String taskId;
}
