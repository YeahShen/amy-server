package site.ashenstation.modules.media.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;
import site.ashenstation.modules.media.service.AesEncryptService;
import site.ashenstation.utils.AesUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StaticResourceEncryptFilter implements Filter {
//    http://localhost:9999/resource/video/795727cc1dc841659d75bb91145d42be/master.m3u8
//    https://amy-apiv6.ashen-station.top/resource/video/146c6dc1e9174df291c8f0bfcfed1258/v480p/segment_000.ts

    private final AesEncryptService aesEncryptService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        if (!uri.endsWith(".ts")) {
            chain.doFilter(request, httpResponse);
            return;
        } else {
            chain.doFilter(request, responseWrapper);
        }


        if (uri.endsWith(".ts")) {

            List<String> list = Arrays.asList(uri.split("/"));
            String parentResId = list.get(3);

            String aesEncryptKey = aesEncryptService.getAesEncryptKey(parentResId);

            byte[] originalBody = responseWrapper.getContentAsByteArray();

            if (originalBody.length > 0) {
                try {
                    byte[] encryptedBody = AesUtils.encrypt(originalBody, aesEncryptKey);

                    responseWrapper.resetBuffer();
                    responseWrapper.getOutputStream().write(encryptedBody);
                    responseWrapper.setContentLength(encryptedBody.length);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            responseWrapper.copyBodyToResponse();
        }


    }
}
