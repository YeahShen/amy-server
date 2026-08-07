package site.ashenstation.amyserver;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@MapperScan("site.ashenstation.amyserver.mapper")
@Slf4j
public class AppRun {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(AppRun.class);
        springApplication.addListeners(new ApplicationPidFileWriter());
        ConfigurableApplicationContext context = springApplication.run(args);
        String port = context.getEnvironment().getProperty("server.port");
        log.info("---------------------------------------------");
        log.info("Local: http://localhost:{}", port);
        log.info("---------------------------------------------");
    }

}
