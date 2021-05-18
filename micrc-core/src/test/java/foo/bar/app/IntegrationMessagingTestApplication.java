package foo.bar.app;

import io.ttyys.micrc.integration.EnableMessagingIntegration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

public class IntegrationMessagingTestApplication {

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = { "io.ttyys.micrc.integration.springboot.fixtures" },
            excludeFilters = { @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
            @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
    @EnableMessagingIntegration(basePackages = { "io.ttyys.micrc.integration.springboot.fixtures" })
    public static class ConfigurationTestApplication {}

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = { "io.ttyys.micrc.integration.route.fixtures" },
            excludeFilters = { @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
    @EnableJpaRepositories("io.ttyys.micrc.integration.route.fixtures")
    @EntityScan("io.ttyys.micrc.integration.route.fixtures")
    @EnableMessagingIntegration(basePackages = { "io.ttyys.micrc.integration.route.fixtures" })
    public static class TransactionTestApplication {}

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = { "io.ttyys.micrc.integration.route.fixtures" },
            excludeFilters = { @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
    @EnableJpaRepositories("io.ttyys.micrc.integration.route.fixtures")
    @EntityScan("io.ttyys.micrc.integration.route.fixtures")
    @EnableMessagingIntegration(basePackages = { "io.ttyys.micrc.integration.route.fixtures" })
    public static class PriorityTestApplication {}
}
