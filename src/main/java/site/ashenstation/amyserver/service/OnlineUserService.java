package site.ashenstation.amyserver.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.ashenstation.amyserver.dto.OnlineUserDto;
import site.ashenstation.amyserver.enums.LoginPlatform;
import site.ashenstation.amyserver.property.SecurityProperties;
import site.ashenstation.amyserver.utils.IpAddrUtils;
import site.ashenstation.amyserver.utils.RedisUtils;
import site.ashenstation.amyserver.utils.TokenProvider;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineUserService {
    private final SecurityProperties securityProperties;
    private final TokenProvider tokenProvider;
    private final RedisUtils redisUtils;

    public void save(String username, String token, HttpServletRequest request) {
        String ip = IpAddrUtils.getIp(request);
        String address = IpAddrUtils.getCityInfo(ip);
        OnlineUserDto onlineUserDto;

        try {
            onlineUserDto = new OnlineUserDto();
            onlineUserDto.setIp(ip);
            onlineUserDto.setUserName(username);
            onlineUserDto.setAddress(address);
            onlineUserDto.setLoginTime(new Date());

            String loginKey = tokenProvider.loginKey(token);

            redisUtils.set(loginKey, onlineUserDto, securityProperties.getTokenValidityInSeconds(), TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void save(String username, String token, HttpServletRequest request, LoginPlatform loginPlatform) {
        String ip = IpAddrUtils.getIp(request);
        String address = IpAddrUtils.getCityInfo(ip);
        OnlineUserDto onlineUserDto;

        try {
            onlineUserDto = new OnlineUserDto();
            onlineUserDto.setIp(ip);
            onlineUserDto.setUserName(username);
            onlineUserDto.setAddress(address);
            onlineUserDto.setLoginTime(new Date());

            onlineUserDto.setLoginPlatform(loginPlatform);

            String loginKey = tokenProvider.loginKey(token, onlineUserDto.getLoginPlatform());

            redisUtils.set(loginKey, onlineUserDto, securityProperties.getTokenValidityInSeconds(), TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 退出登录
     *
     * @param token /
     */
    public void logout(String token) {
        String loginKey = tokenProvider.loginKey(token);
        redisUtils.del(loginKey);
    }

    public void logout(String token, LoginPlatform loginPlatform) {
        String loginKey = tokenProvider.loginKey(token, loginPlatform);
        redisUtils.del(loginKey);
    }


    /**
     * 查询用户
     *
     * @param key /
     * @return /
     */
    public OnlineUserDto getOne(String key) {
        return redisUtils.get(key, OnlineUserDto.class);
    }

    /**
     * 根据用户名强退用户
     *
     * @param username /
     */
    public void kickOutForUsername(String username) {
        String loginKey = securityProperties.getOnlineKey() + username + "*";
        redisUtils.scanDel(loginKey);
    }

    public void kickOutForUsernameAndPlatform(String username, LoginPlatform platform) {
        String loginKey = securityProperties.getOnlineKey() + platform.getType() + ":" + username + "*";
        redisUtils.scanDel(loginKey);
    }
}
