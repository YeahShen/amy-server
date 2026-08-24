package site.ashenstation.amyserver.vo;

import lombok.Data;
import lombok.ToString;
import site.ashenstation.amyserver.entity.Artist;

import java.util.List;

@Data
@ToString
public class ArtistByCategoryVo {
    private String id;
    private String title;
    private List<Artist> list;

    public void processAvatarUrl(String prefix) {
        this.list.forEach(artist -> {
            artist.setAvatar(prefix + "/" + artist.getAvatar());
        });
    }
}
