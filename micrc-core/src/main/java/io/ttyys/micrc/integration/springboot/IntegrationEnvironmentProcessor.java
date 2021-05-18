package io.ttyys.micrc.integration.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

@Order
class IntegrationEnvironmentProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Properties properties = new Properties();
        properties.put("spring.datasource.url", "test");
        PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource("test", properties);
        environment.getPropertySources().addLast(propertiesPropertySource);
        System.out.println(environment);
    }
}
