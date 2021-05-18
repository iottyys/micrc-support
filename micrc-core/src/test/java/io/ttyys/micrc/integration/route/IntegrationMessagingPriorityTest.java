package io.ttyys.micrc.integration.route;

import foo.bar.app.IntegrationMessagingTestApplication;
import io.ttyys.micrc.integration.EnableMessagingIntegration;
import io.ttyys.micrc.integration.route.fixtures.CustomerRepository;
import io.ttyys.micrc.integration.route.fixtures.DemoTxMessageAdapter;
import io.ttyys.micrc.integration.route.fixtures.Message;
import org.apache.camel.Produce;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;

@CamelSpringBootTest
@MockEndpoints("direct:end*")
@SpringBootTest(classes = {
        IntegrationMessagingPriorityTest.ConfigurationTestConfiguration.class,
        IntegrationMessagingTestApplication.PriorityTestApplication.class,
        DemoTxMessageAdapter.class
}, properties = { "logging.level.root=DEBUG" })
public class IntegrationMessagingPriorityTest {

    @Autowired
    private DemoTxMessageAdapter messageAdapter;

    @Test
    public void test() throws InterruptedException {
        messageAdapter.sendMsg100Only();
        new CountDownLatch(1).await();
    }

    @Configuration
    @Import(DataSourceAutoConfiguration.class)
    static class ConfigurationTestConfiguration extends RouteBuilder {

        @Override
        public void configure() {

        }
    }
}
