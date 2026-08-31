package site.ashenstation.amyserver.dto;

import lombok.Data;
import lombok.ToString;
import site.ashenstation.amyserver.enums.UploadTaskType;

@Data
@ToString
public class UploadTaskDto<T> {
    private String id;
    private UploadTaskType type;
    private T data;
}
