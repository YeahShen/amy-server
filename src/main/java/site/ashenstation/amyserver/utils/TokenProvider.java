package site.ashenstation.amyserver.utils;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import site.ashenstation.amyserver.enums.LoginPlatform;
import site.ashenstation.amyserver.property.SecurityProperties;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider implements InitializingBean {

    private JwtParser jwtParser;
    private JwtBuilder jwtBuilder;
    private final RedisUtils redisUtils;
    private final SecurityProperties securityProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        byte[] keyBytes = Decoders.BASE64.decode(securityProperties.getBase64Secret());
        Key key = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parserBuilder()
                .setSigningKey(key)
                .build();
        jwtBuilder = Jwts.builder()
                .signWith(key, SignatureAlgorithm.HS512);

        log.info("TokenProvider initialized");
    }

    /**
     * 创建Token 设置永不过期，
     * Token 的时间有效性转到Redis 维护
     */
    public String createToken(String subject, Map<String, String> claims) {
        return jwtBuilder
                .setClaims(claims)
                .setSubject(subject)
                .compact();
    }

    /**
     * 创建Token 设置永不过期，
     *
     * @param subject subject
     * @param claims  claims
     * @param expired 过期时间毫秒
     * @return token
     */
    public String createToken(String subject, Map<String, String> claims, Long expired) {
        return jwtBuilder
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expired))
                .compact();
    }

    public Claims getClaims(String token) {
        return jwtParser
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * @param token 需要检查的token
     */
    public void checkRenewal(String token) {
        // 判断是否续期token,计算token的过期时间
        String loginKey = loginKey(token);
        long time = redisUtils.getExpire(loginKey) * 1000;
        Date expireDate = DateUtil.offset(new Date(), DateField.MILLISECOND, (int) time);
        // 判断当前时间与过期时间的时间差
        long differ = expireDate.getTime() - System.currentTimeMillis();
        // 如果在续期检查的范围内，则续期
        if (differ <= securityProperties.getDetect()) {
            long renew = time + securityProperties.getRenew();
            redisUtils.expire(loginKey, renew, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 获取登录用户RedisKey
     *
     * @param token /
     * @return key
     */
    public String loginKey(String token) {
        Claims claims = getClaims(token);
        return securityProperties.getOnlineKey() + claims.getSubject() + ":" + getId(token);
    }

    public String loginKey(String token, LoginPlatform loginPlatform) {
        Claims claims = getClaims(token);
        return securityProperties.getOnlineKey() + loginPlatform.getType() + ":" + claims.getSubject() + ":" + getId(token);
    }

    /**
     * 获取会话编号
     *
     * @param token /
     * @return /
     */
    public String getId(String token) {
        Claims claims = getClaims(token);
        return claims.get(AmyConstants.JWT_CLAIM_UID, String.class);
    }

    /**
     * 检查JWT是否已过期
     *
     * @param token JWT令牌
     * @return 如果已过期返回true，否则返回false
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            Date expiration = claims.getExpiration();

            if (expiration == null)
                return false;
            else
                return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 初步检测Token
     *
     * @param request /
     * @return /
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(securityProperties.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(securityProperties.getTokenStartWith())) {
            // 去掉令牌前缀
            return bearerToken.replace(securityProperties.getTokenStartWith(), "");
        } else {
            log.debug("非法Token：{}", bearerToken);
        }
        return null;
    }

    public String resolveToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        List<String> bearerTokens = headers.get(securityProperties.getHeader());
        assert bearerTokens != null;
        if (bearerTokens.size() == 1) {
            String bearerToken = bearerTokens.get(0);

            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(securityProperties.getTokenStartWith())) {
                // 去掉令牌前缀
                return bearerToken.replace(securityProperties.getTokenStartWith(), "");
            } else {
                log.debug("非法Token：{}", bearerToken);
            }
        }
        return null;
    }
}
