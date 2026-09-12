package site.ashenstation.modules.hugefileupload.dto;

import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Data
@ToString
public class UploadChunkDto {
    private MultipartFile chunk;
    private Integer index;
    private String id;
}
