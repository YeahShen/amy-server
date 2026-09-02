package site.ashenstation.amyserver.config.aspect;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class UploadProcessAspect {

    @AfterReturning(value = "@annotation(site.ashenstation.amyserver.annotation.UploadProcess)", returning = "id")
    public void doUploadProcess(String id) {
        System.out.println("【返回通知】方法返回结果：" + id);
    }
}
