package site.ashenstation.modules.hugefileupload.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CheckChunkDto {
    private String taskId;
    private String totalChunk;
}
