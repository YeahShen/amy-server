package site.ashenstation.modules.media.dto;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UploadProcessorKeyDto extends CreateVideoDto {
    private String taskId;
    private String userId;
}
