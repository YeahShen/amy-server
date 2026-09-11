package site.ashenstation.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TemplateRenderUtils {
    private final Configuration freemarkerConfig;

    public String render(String templateName, Map<String, Object> dataModel) throws Exception {
        Template template = freemarkerConfig.getTemplate(templateName);
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, dataModel);
    }
}
