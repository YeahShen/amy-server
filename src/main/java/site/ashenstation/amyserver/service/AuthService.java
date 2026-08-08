package site.ashenstation.amyserver.service;

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
import site.ashenstation.amyserver.property.RsaProperties;
import site.ashenstation.amyserver.utils.RsaUtils;
import site.ashenstation.amyserver.utils.TokenProvider;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    public Map<String, Object> loginByUsernamePassword(AuthByUsernamePasswordDto dto, HttpServletRequest request) {

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

        System.out.println(jwtUserDto);

        return new HashMap<>() {{
        }};
    }
}
