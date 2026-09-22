package site.ashenstation.modules.security.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import site.ashenstation.enums.LoginPlatform;
import site.ashenstation.exception.BadRequestException;
import site.ashenstation.modules.security.dto.OnlineUserDto;
import site.ashenstation.modules.security.service.OnlineUserService;
import site.ashenstation.utils.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TokenFilter extends GenericFilterBean {

    private final TokenProvider tokenProvider;
    private final OnlineUserService onlineUserService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        String token = tokenProvider.resolveToken(httpServletRequest);

        if (StringUtils.hasText(token)) {
            boolean tokenExpired = tokenProvider.isTokenExpired(token);

            if (tokenExpired) {
                throw new BadRequestException("token expired");
            }

            Claims claims = tokenProvider.getClaims(token);
            String userId = claims.get(Constants.JWT_CLAIM_USER_ID, String.class);

            String platformVal = claims.get(Constants.JWT_CLAIM_PLATFORM, String.class);
            String loginKey = tokenProvider.loginKey(token, Objects.requireNonNull(LoginPlatform.find(platformVal)));

            OnlineUserDto onlineUserDto = onlineUserService.getOne(loginKey);

            if (onlineUserDto != null) {
                // 会话仍在线，写入 SecurityContext；principal 为无权限的 Spring User，authorities 为空
                ArrayList<GrantedAuthority> grantedAuthorities = new ArrayList<>();

                User principal = new User(claims.getSubject(), "******", grantedAuthorities);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, token, grantedAuthorities));
            }
        }

        chain.doFilter(request, response);
    }
}
