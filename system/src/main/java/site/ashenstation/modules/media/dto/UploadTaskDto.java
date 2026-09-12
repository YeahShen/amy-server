package site.ashenstation.modules.media.dto;

import lombok.Data;
import lombok.ToString;
import site.ashenstation.enums.UploadTaskType;

@Data
@ToString
public class UploadTaskDto<T> {
    private String id;
    private UploadTaskType type;
    private T data;
}
