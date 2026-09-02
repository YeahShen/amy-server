package site.ashenstation.amyserver.config.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import site.ashenstation.amyserver.dto.UploadProcessorKeyDto;
import site.ashenstation.amyserver.service.UploadService;


@Aspect
@Component
@RequiredArgsConstructor
public class UploadProcessAspect {

    private final UploadService uploadService;

    @AfterReturning(value = "@annotation(site.ashenstation.amyserver.annotation.UploadProcess)", returning = "data")
    public void doUploadProcess(UploadProcessorKeyDto data) {
        System.out.println("【返回通知】方法返回结果：" + data);
        uploadService.nextStep(data);
    }
}
