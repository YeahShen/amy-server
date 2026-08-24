package site.ashenstation.amyserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import site.ashenstation.amyserver.entity.ArtistCategory;

@Data
@ToString
public class ArtistDto {
    @NotBlank(message = "艺术家名称不能为空")
    private String name;
    private String description;
    private MultipartFile avatarFile;
    @NotNull(message = "艺术家分类不能为空")
    private ArtistCategory category;
}
