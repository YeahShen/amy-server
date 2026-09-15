package site.ashenstation.modules.media.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.ashenstation.infrastructure.property.StaticResourceDirectoryProperties;

@Configuration
@RequiredArgsConstructor
public class SRFilterConfig {

    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;
    private final StaticResourceEncryptFilter staticResourceEncryptFilter;

    @Bean
    public FilterRegistrationBean<StaticResourceEncryptFilter> encryptFilterRegistration() {
        FilterRegistrationBean<StaticResourceEncryptFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(staticResourceEncryptFilter); // 必须设置实例
        registration.addUrlPatterns(staticResourceDirectoryProperties.getVideoResourcePrefix() + "/*"); // 根据你的路径调整
        registration.setName("staticResourceEncryptFilter");
        registration.setOrder(1); // 控制执行顺序
        return registration;
    }
}
