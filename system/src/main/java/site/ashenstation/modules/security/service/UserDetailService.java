package site.ashenstation.modules.security.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import site.ashenstation.infrastructure.dao.AppUserMapper;
import site.ashenstation.model.entity.AppUser;
import site.ashenstation.model.entity.table.AppUserTableDef;
import site.ashenstation.modules.security.dto.JwtAppUserDto;

@RequiredArgsConstructor
@Service
public class UserDetailService implements UserDetailsService {
    private final UserCacheService userCacheService;
    private final AppUserMapper userMapper;

    /**
     * 按用户名加载用户：先查 Redis 缓存，未命中则查库并异步回写缓存
     *
     * @param username 用户名
     * @return 用户信息
     * @throws UsernameNotFoundException 用户名不存在
     */
    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        JwtAppUserDto jwtUserDto = (JwtAppUserDto) userCacheService.getUserCache(username);


        if (jwtUserDto == null) {
            AppUser user = userMapper.selectOneByCondition(
                    AppUserTableDef.APP_USER.USERNAME.eq(username));
            if (user == null) {
                throw new UsernameNotFoundException(username);
            }

            jwtUserDto = new JwtAppUserDto();
            jwtUserDto.setAppUser(user);

            userCacheService.addUserCache(username, jwtUserDto);
        }

        return jwtUserDto;
    }
}
