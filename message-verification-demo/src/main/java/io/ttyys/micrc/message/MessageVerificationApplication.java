package io.ttyys.micrc.message;

import io.ttyys.micrc.integration.local.springboot.EnableLocalMessageSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"io.ttyys"})
@EnableLocalMessageSupport(servicePackages = "io.ttyys")
public class MessageVerificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageVerificationApplication.class, args);
    }
}
