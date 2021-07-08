package io.ttyys.micrc.api;

import io.ttyys.micrc.api.route.ApiRouteConfiguration;
import io.ttyys.micrc.api.route.ApiRouteTemplateParameterSource;
import org.apache.camel.CamelContext;
import org.apache.camel.component.bean.validator.HibernateValidationProviderResolver;
import org.apache.camel.spi.RouteTemplateParameterSource;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAutoConfiguration
@Import({ApiRouteConfiguration.class, ApiAspect.class})
public class ApiAutoConfiguration {
    @Bean
    public ApiRouteTemplateParameterSource fakeApiRouteTemplateParameterSource() {
        return new ApiRouteTemplateParameterSource();
    }

    @Bean("io.ttyys.micrc.api.route.ApiRouteConfiguration.contextConfiguration")
    @Order
    public CamelContextConfiguration contextConfiguration(ApiRouteTemplateParameterSource source) {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                camelContext.getRegistry().bind("ApiRouteTemplateParametersSource",
                        RouteTemplateParameterSource.class, source);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
            }
        };
    }

    @Bean
    public HibernateValidationProviderResolver validationProviderResolver() {
        return new HibernateValidationProviderResolver();
    }
}
