package site.ashenstation.amyserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import site.ashenstation.amyserver.entity.Video;
import site.ashenstation.amyserver.entity.VideoArtistMap;
import site.ashenstation.amyserver.entity.VideoTagMap;

import java.util.ArrayList;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class VideoTemporaryInformationDto {
    private ArrayList<VideoArtistMap> videoArtistMaps;
    private ArrayList<VideoTagMap> videoTagMaps;
    private Video video;
}
