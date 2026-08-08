package site.ashenstation.amyserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import site.ashenstation.amyserver.dto.JwtUserDto;
import site.ashenstation.amyserver.entity.User;
import site.ashenstation.amyserver.entity.table.UserTableDef;
import site.ashenstation.amyserver.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService {

    private final UserCacheManager userCacheManager;
    private final UserMapper userMapper;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        JwtUserDto jwtUserDto = (JwtUserDto) userCacheManager.getUserCache(username);

        if (jwtUserDto == null) {
            User user = userMapper.selectOneByCondition(UserTableDef.USER.USERNAME.eq(username));
            if (user == null) {
                throw new UsernameNotFoundException(username);
            }

            jwtUserDto = new JwtUserDto(user);

            userCacheManager.addUserCache(username, jwtUserDto);
        }


        return jwtUserDto;
    }
}
