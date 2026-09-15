package site.ashenstation.infrastructure.config.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import site.ashenstation.infrastructure.property.StaticResourceDirectoryProperties;

@Configuration
@EnableWebMvc
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final StaticResourceDirectoryProperties staticResourceDirectoryProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String userAvatarUtl = "file:" + staticResourceDirectoryProperties.getUserAvatarDirectory().replace("\\", "/") + "/";
        String artistAvatarUtl = "file:" + staticResourceDirectoryProperties.getArtistAvatarDirectory().replace("\\", "/") + "/";

        String posterUtl = "file:" + staticResourceDirectoryProperties.getPosterDirectory().replace("\\", "/") + "/";

        registry.addResourceHandler(staticResourceDirectoryProperties.getUserAvatarPathPrefix() + "/**").addResourceLocations(userAvatarUtl);
        registry.addResourceHandler(staticResourceDirectoryProperties.getArtistAvatarPathPrefix() + "/**").addResourceLocations(artistAvatarUtl);

        registry.addResourceHandler(staticResourceDirectoryProperties.getPosterPathPrefix() + "/**").addResourceLocations(posterUtl);

        ResourceHandlerRegistration resourceHandlerRegistration = registry.addResourceHandler(staticResourceDirectoryProperties.getVideoResourcePrefix() + "/**");

        staticResourceDirectoryProperties.getVideoRoots().forEach(resource -> {
            resourceHandlerRegistration.addResourceLocations("file:" + resource.getPath().replace("\\", "/") + "/");
        });
    }


}
