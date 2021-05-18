package io.ttyys.micrc.integration.route;

import io.ttyys.micrc.integration.EnableMessagingIntegration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@CamelSpringBootTest
@SpringBootApplication
@EnableMessagingIntegration
public class IntegrationMessagingDuplicateTest {
}
