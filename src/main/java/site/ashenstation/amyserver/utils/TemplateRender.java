package site.ashenstation.amyserver.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import site.ashenstation.amyserver.property.FFmpegProperties;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TemplateRender {
    private final Configuration freemarkerConfig;
    private final FFmpegProperties fFmpegProperties;

    public String render(String templateName, Map<String, Object> dataModel) throws Exception {
        Template template = freemarkerConfig.getTemplate(templateName);
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, dataModel);
    }

    @PostConstruct
    private void ttt() throws Exception {
        HashMap<String, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("videoEncoder", fFmpegProperties.getVideoEncoder());
        objectObjectHashMap.put("audioEncoder", fFmpegProperties.getAudioEncoder());

        String render = this.render("ffmpeg/v1080p_hls.ftlh", objectObjectHashMap);
        System.out.println(render.replaceAll("\\\\[ \\t]*\\r?\\n", " ")
                .replaceAll("\\s+", " ")
                .trim());
    }
}
