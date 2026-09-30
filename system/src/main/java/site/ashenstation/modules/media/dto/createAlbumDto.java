package site.ashenstation.modules.media.dto;

import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Data
@ToString
public class createAlbumDto {
    private String name;
    private String description;
    private String artistId;
    private MultipartFile coverPhoto;
}
