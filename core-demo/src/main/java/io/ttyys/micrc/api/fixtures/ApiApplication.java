package io.ttyys.micrc.api.fixtures;


import io.ttyys.micrc.api.EnableApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"io.ttyys.micrc.api.fixtures"})
@EnableApi(basePackages = {"io.ttyys.micrc.api.fixtures"})
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}

