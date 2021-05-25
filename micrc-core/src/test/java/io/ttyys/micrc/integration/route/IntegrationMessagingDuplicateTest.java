package io.ttyys.micrc.integration.route;

import foo.bar.app.IntegrationMessagingTestApplication;
import io.ttyys.micrc.integration.route.fixtures.DemoTxMessageAdapter;
import io.ttyys.micrc.integration.route.fixtures.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@CamelSpringBootTest
@MockEndpoints("direct:end*")
@SpringBootTest(classes = {
        IntegrationMessagingDuplicateTest.ConfigurationTestConfiguration.class,
        IntegrationMessagingTestApplication.DuplicateTestApplication.class,
        DemoTxMessageAdapter.class
}, properties = { "logging.level.root=DEBUG" })
public class IntegrationMessagingDuplicateTest {

    @Autowired
    private ProducerTemplate template;

    @Test
    public void test() throws InterruptedException {
    }

    @Configuration
    static class ConfigurationTestConfiguration extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:avro").marshal().avro(Message.class.getName());
        }
    }
}
