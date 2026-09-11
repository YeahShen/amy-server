package site.ashenstation;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
@Slf4j
@EnableAsync
@RestController
@MapperScan("site.ashenstation.infrastructure.dao")
public class Booter {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Booter.class);
        springApplication.addListeners(new ApplicationPidFileWriter());
        ConfigurableApplicationContext context = springApplication.run(args);
        String port = context.getEnvironment().getProperty("server.port");
        log.info("---------------------------------------------");
        log.info("Local: http://localhost:{}", port);
        log.info("---------------------------------------------");
    }

    @GetMapping("/")
    public String index() {
        return "AMY STATION @";
    }
}
