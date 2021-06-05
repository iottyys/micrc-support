package io.ttyys.micrc.integration.route;

import foo.bar.app.IntegrationMessagingTestApplication;
import io.ttyys.micrc.integration.route.fixtures.DemoTxMessageAdapter;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;
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
        IntegrationMessagingOrderTest.ConfigurationTestConfiguration.class,
        IntegrationMessagingTestApplication.OrderTestApplication.class,
        DemoTxMessageAdapter.class
}, properties = { "logging.level.org.apache.activemq=DEBUG" })
public class IntegrationMessagingOrderTest {

    @Autowired
    private DemoTxMessageAdapter messageAdapter;



    @Test
    public void test() throws InterruptedException {
        EmbeddedActiveMQResource server = new EmbeddedActiveMQResource();
        server.start();
//        messageAdapter.sendMsg100Only();
//        new CountDownLatch(1).await();
    }

    @Configuration
    static class ConfigurationTestConfiguration extends RouteBuilder {

        @Override
        public void configure() {

        }
    }
}
