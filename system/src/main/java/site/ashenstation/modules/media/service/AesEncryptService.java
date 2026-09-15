package site.ashenstation.modules.media.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import site.ashenstation.infrastructure.dao.ResourceAesEncryptMapper;
import site.ashenstation.model.entity.ResourceAesEncrypt;
import site.ashenstation.model.entity.table.ResourceAesEncryptTableDef;

@Service
@RequiredArgsConstructor
public class AesEncryptService {

    private final ResourceAesEncryptMapper encryptMapper;

    @Cacheable(value = "users", key = "#resourceId")
    public String getAesEncryptKey(String resourceId) {
        ResourceAesEncrypt resourceAesEncrypt = encryptMapper.selectOneByCondition(ResourceAesEncryptTableDef.RESOURCE_AES_ENCRYPT.RESOURCE_ID.eq(resourceId));
        assert resourceAesEncrypt != null;
        return resourceAesEncrypt.getEncryptKey();
    }
}
