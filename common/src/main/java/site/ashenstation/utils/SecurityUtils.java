package site.ashenstation.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class SecurityUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String header;

    public static String tokenStartWith;

    @Value("${jwt.header}")
    public void setHeader(String header) {
        SecurityUtils.header = header;
    }

    @Value("${jwt.token-start-with}")
    public void setTokenStartWith(String tokenStartWith) {
        SecurityUtils.tokenStartWith = tokenStartWith;
    }

    /**
     * 获取当前登录的用户
     *
     * @return UserDetails，用户不存在返回 null
     */
    public static @Nullable UserDetails getCurrentUser() {
        UserDetailsService userDetailsService = SpringBeanHolder.getBean(UserDetailsService.class);
        return userDetailsService.loadUserByUsername(getCurrentUsername());
    }

    /**
     * 获取当前用户的数据权限
     *
     * @return /
     */
    public static List<Long> getCurrentUserDataScope() {
        UserDetails userDetails = getCurrentUser();
        // 将 Java 对象转换为 JsonNode 对象
        JsonNode jsonNode = OBJECT_MAPPER.convertValue(userDetails, JsonNode.class);
        JsonNode dataScopes = jsonNode.get("dataScopes");
        if (dataScopes == null || dataScopes.isNull()) {
            return new ArrayList<>();
        }
        return OBJECT_MAPPER.convertValue(dataScopes, new TypeReference<List<Long>>() {
        });
    }

    /**
     * 获取用户ID
     *
     * @return 系统用户ID
     */
    public static String getCurrentUserId() {
        return getCurrentUserId(getToken());
    }

    public static String getTokenUid() {
        String token = getToken();
        JWT jwt = JWTUtil.parseToken(token);
        return jwt.getPayload(Constants.JWT_CLAIM_UID).toString();
    }

    /**
     * 获取用户ID
     *
     * @return 系统用户ID
     */
    public static String getCurrentUserId(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        return jwt.getPayload(Constants.JWT_CLAIM_USER_ID).toString();
    }

    /**
     * 获取系统用户名称
     *
     * @return 系统用户名称
     */
    public static String getCurrentUsername() {
        return getCurrentUsername(getToken());
    }

    /**
     * 获取系统用户名称
     *
     * @return 系统用户名称
     */
    public static String getCurrentUsername(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        return jwt.getPayload("sub").toString();
    }

    /**
     * 获取Token
     *
     * @return /
     */
    public static String getToken() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder
                .getRequestAttributes())).getRequest();
        String bearerToken = request.getHeader(header);
        if (bearerToken != null && bearerToken.startsWith(tokenStartWith)) {
            // 去掉令牌前缀
            return bearerToken.replace(tokenStartWith, "");
        } else {
            log.debug("非法Token：{}", bearerToken);
        }
        return null;
    }
}
