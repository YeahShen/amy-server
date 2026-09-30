package site.ashenstation.modules.media.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.ashenstation.infrastructure.dao.AlbumMapper;
import site.ashenstation.modules.media.dto.createAlbumDto;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumMapper albumMapper;

    public void createAlbum(createAlbumDto dto) {
    }
}
