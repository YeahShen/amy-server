package site.ashenstation.amyserver.service;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.util.UpdateEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import site.ashenstation.amyserver.config.exception.BadRequestException;
import site.ashenstation.amyserver.dto.AuthByUsernamePasswordDto;
import site.ashenstation.amyserver.dto.JwtUserDto;
import site.ashenstation.amyserver.entity.User;
import site.ashenstation.amyserver.enums.LoginPlatform;
import site.ashenstation.amyserver.mapper.UserMapper;
import site.ashenstation.amyserver.property.LoginProperties;
import site.ashenstation.amyserver.property.RsaProperties;
import site.ashenstation.amyserver.property.SecurityProperties;
import site.ashenstation.amyserver.utils.*;
import site.ashenstation.amyserver.vo.AuthResVo;

import java.util.Date;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final SecurityProperties securityProperties;
    private final UserMapper userMapper;
    private final OnlineUserService onlineUserService;
    private final LoginProperties loginProperties;

    public AuthResVo loginByUsernamePassword(AuthByUsernamePasswordDto dto, HttpServletRequest request) {

        String password;

        try {
            password = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, dto.getPassword());
        } catch (Exception e) {
            throw new BadRequestException("密码错误");
        }

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken(dto.getUsername(), password);

        Authentication authenticate =
                authenticationManager.authenticate(usernamePasswordAuthenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authenticate);

        JwtUserDto jwtUserDto = (JwtUserDto) authenticate.getPrincipal();

        assert jwtUserDto != null;
        User user = jwtUserDto.getUser();

        user.setPassword(null);
        LoginPlatform loginPlatform = LoginPlatform.find(request.getHeader(securityProperties.getClientHeader()));

        HashMap<String, String> claims = new HashMap<>() {{
            put(AmyConstants.JWT_CLAIM_USERNAME, dto.getUsername());
            put(AmyConstants.JWT_CLAIM_USER_ID, String.valueOf(user.getId()));
            put(AmyConstants.JWT_CLAIM_UID, IdUtil.fastSimpleUUID());
            assert loginPlatform != null;
            put(AmyConstants.JWT_CLAIM_PLATFORM, loginPlatform.getType());
        }};

        String token = tokenProvider.createToken(user.getUsername(), claims, securityProperties.getTokenValidityInSeconds());

        User updateUser = UpdateEntity.of(User.class, user.getId());
        updateUser.setLastLoginTime(new Date());

        String ip = IpAddrUtils.getIp(request);
        updateUser.setLastLoginIp(ip);

        userMapper.update(updateUser);


        if (loginProperties.isSingleLogin()) {
            assert loginPlatform != null;
            onlineUserService.kickOutForUsernameAndPlatform(user.getUsername(), loginPlatform);
        }

        onlineUserService.save(user.getUsername(), token, request, loginPlatform);

        return new AuthResVo(token, user);
    }

    public User getInfo() {
        JwtUserDto jwtUser = (JwtUserDto) SecurityUtils.getCurrentUser();
        User appUser = jwtUser.getUser();

        appUser.setPassword(null);
        return appUser;
    }

    public Boolean logout(HttpServletRequest request) {
        String token = tokenProvider.resolveToken(request);

        LoginPlatform loginPlatform = LoginPlatform.find(request.getHeader(securityProperties.getClientHeader()));

        onlineUserService.logout(token, loginPlatform);

//        Claims claims = tokenProvider.getClaims(token);
//        String uid = claims.get(AmyConstants.JWT_CLAIM_UID, String.class);
//        String userId = claims.get(AmyConstants.JWT_CLAIM_USER_ID, String.class);

//        try {
//            WebSocketSession webSocketSession = WebSocketHandler.ONLINE_SESSIONS.get(userId + ":" + uid);
//            WebSocketHandler.ONLINE_SESSIONS.remove(userId + ":" + uid);
//            webSocketSession.close();
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
        return true;
    }

}
