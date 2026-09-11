package site.ashenstation.modules.security.rest;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.util.UpdateEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import site.ashenstation.annotation.rest.AnonymousPostMapping;
import site.ashenstation.enums.LoginPlatform;
import site.ashenstation.exception.BadRequestException;
import site.ashenstation.infrastructure.dao.AppUserMapper;
import site.ashenstation.infrastructure.property.LoginProperties;
import site.ashenstation.infrastructure.property.RsaProperties;
import site.ashenstation.infrastructure.property.SecurityProperties;
import site.ashenstation.infrastructure.property.StaticResourceDirectoryProperties;
import site.ashenstation.model.entity.AppUser;
import site.ashenstation.modules.security.config.TokenProvider;
import site.ashenstation.modules.security.dto.AuthDto;
import site.ashenstation.modules.security.dto.JwtAppUserDto;
import site.ashenstation.modules.security.service.OnlineUserService;
import site.ashenstation.modules.security.vo.UserInfoVo;
import site.ashenstation.utils.Constants;
import site.ashenstation.utils.IpAddrUtils;
import site.ashenstation.utils.RsaUtils;
import site.ashenstation.utils.SecurityUtils;

import java.util.Date;
import java.util.HashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;
    private final SecurityProperties securityProperties;
    private final AppUserMapper appUserMapper;
    private final OnlineUserService onlineUserService;
    private final LoginProperties loginProperties;

    @AnonymousPostMapping("login-by-username-password")
    public ResponseEntity<UserInfoVo> loginByUsername(@RequestBody AuthDto dto, HttpServletRequest request) {
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

        JwtAppUserDto jwtUserDto = (JwtAppUserDto) authenticate.getPrincipal();

        assert jwtUserDto != null;

        AppUser user = jwtUserDto.getAppUser();

        user.setPassword(null);
        user.setAvatar(staticResourceDirectoryProperties.getUserAvatarPathPrefix() + "/" + user.getAvatar());

        LoginPlatform loginPlatform = LoginPlatform.find(request.getHeader(securityProperties.getClientHeader()));

        HashMap<String, String> claims = new HashMap<>() {{
            put(Constants.JWT_CLAIM_USERNAME, dto.getUsername());
            put(Constants.JWT_CLAIM_USER_ID, String.valueOf(user.getId()));
            put(Constants.JWT_CLAIM_UID, IdUtil.fastSimpleUUID());
            assert loginPlatform != null;
            put(Constants.JWT_CLAIM_PLATFORM, loginPlatform.getType());
        }};

        String token = tokenProvider.createToken(user.getUsername(), claims, securityProperties.getTokenValidityInSeconds());

        AppUser updateUser = UpdateEntity.of(AppUser.class, user.getId());
        updateUser.setLastLoginTime(new Date());

        String ip = IpAddrUtils.getIp(request);
        updateUser.setLastLoginIp(ip);

        appUserMapper.update(updateUser);


        if (loginProperties.isSingleLogin()) {
            assert loginPlatform != null;
            onlineUserService.kickOutForUsernameAndPlatform(user.getUsername(), loginPlatform);
        }

        onlineUserService.save(user.getUsername(), token, request, loginPlatform);

        return ResponseEntity.ok(new UserInfoVo(token, user));
    }


    @GetMapping("/info")
    public ResponseEntity<AppUser> getInfo() {
        JwtAppUserDto jwtUser = (JwtAppUserDto) SecurityUtils.getCurrentUser();
        if (jwtUser == null) {
            throw new BadRequestException("登录状态已失效");
        }

        AppUser appUser = jwtUser.getAppUser();
        appUser.setPassword(null);
        return ResponseEntity.ok(appUser);
    }


    @DeleteMapping("/logout")
    public ResponseEntity<Boolean> logout(HttpServletRequest request) {
        String token = tokenProvider.resolveToken(request);

        LoginPlatform loginPlatform = LoginPlatform.find(request.getHeader(securityProperties.getClientHeader()));

        onlineUserService.logout(token, loginPlatform);

        return ResponseEntity.ok(true);
    }
}
