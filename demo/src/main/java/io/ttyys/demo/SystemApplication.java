package io.ttyys.demo;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.InetAddress;

@Slf4j
@EnableSwagger2
@SpringBootApplication(scanBasePackages = {"io.ttyys.demo"})
//@EnableApi(basePackages = {"io.ttyys.demo"})
public class SystemApplication {
    @SneakyThrows
    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(SystemApplication.class, args);
        Environment env = application.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String path = env.getProperty("server.servlet.context-path");
        log.info("\n----------------------------------------------------------\n    " +
                "Application Demo-Boot is running! Access URLs:\n    " +
                "Local:       http://localhost:" + port + path + "/doc.html\n    " +
                "External:    http://" + ip + ":" + port + path + "/doc.html\n    " +
                "Swagger文档:  http://" + ip + ":" + port + path + "/swagger-ui.html\n" +
                "----------------------------------------------------------");

    }
}

