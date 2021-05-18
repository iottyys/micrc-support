package io.ttyys.micrc.integration;

import io.ttyys.micrc.integration.springboot.ClassPathIntegrationMessagingScannerRegistrar;
import io.ttyys.micrc.integration.springboot.IntegrationMessagingAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({ IntegrationMessagingAutoConfiguration.class, ClassPathIntegrationMessagingScannerRegistrar.class })
public @interface EnableMessagingIntegration {
    String[] basePackages() default {};
}
