package site.ashenstation.modules.security.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import site.ashenstation.model.entity.AppUser;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Data
public class JwtAppUserDto implements UserDetails, Serializable {
    private AppUser appUser;

    /**
     * 该属性只有 getter、无 setter 与字段，而 Redis 序列化开启了 @class 多态类型信息，
     * 反序列化时会报 "no way to handle typed deser with setterless yet"，故忽略。
     * 权限恒为空，不进缓存也不影响鉴权。
     */
    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable String getPassword() {
        return this.appUser.getPassword();
    }

    @NullMarked
    @Override
    public String getUsername() {
        return this.appUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return Boolean.TRUE.equals(this.appUser.getAccountNonExpired());
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(this.appUser.getAccountNonLocked());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return Boolean.TRUE.equals(this.appUser.getCredentialsNonExpired());
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.appUser.getEnabled());
    }
}
