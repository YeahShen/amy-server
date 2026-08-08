package site.ashenstation.amyserver.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class SecurityUtils {

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
     * @return UserDetails
     */
    public static UserDetails getCurrentUser() {
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
        // 将 Java 对象转换为 JSONObject 对象
        JSONObject jsonObject = (JSONObject) JSON.toJSON(userDetails);
        JSONArray jsonArray = jsonObject.getJSONArray("dataScopes");
        return JSON.parseArray(jsonArray.toJSONString(), Long.class);
    }

    /**
     * 获取用户ID
     *
     * @return 系统用户ID
     */
    public static String getCurrentUserId() {
        return getCurrentUserId(getToken());
    }

    /**
     * 获取用户ID
     *
     * @return 系统用户ID
     */
    public static String getCurrentUserId(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        return jwt.getPayload(AmyConstants.JWT_CLAIM_USER_ID).toString();
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
