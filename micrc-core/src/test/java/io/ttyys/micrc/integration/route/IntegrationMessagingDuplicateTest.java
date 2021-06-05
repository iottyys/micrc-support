package io.ttyys.micrc.integration.route;

import foo.bar.app.IntegrationMessagingTestApplication;
import io.ttyys.micrc.integration.route.fixtures.DemoTxMessageAdapter;
import io.ttyys.micrc.integration.route.fixtures.Message;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CamelSpringBootTest
@MockEndpoints("direct:end*")
@SpringBootTest(classes = {
        IntegrationMessagingDuplicateTest.ConfigurationTestConfiguration.class,
        IntegrationMessagingTestApplication.DuplicateTestApplication.class,
        DemoTxMessageAdapter.class
}, properties = { "logging.level.root=DEBUG" })
public class IntegrationMessagingDuplicateTest {

    private static final String DUPLICATE_DETECTION_ID =
            StringUtils.replace(UUID.randomUUID().toString().toUpperCase(), "-", "");

    @Autowired
    private ProducerTemplate template;

    @SuppressWarnings("unused")
    @EndpointInject("mock:direct:end")
    private MockEndpoint routeMock;

    @Test
    public void testPublishDuplicate() throws InterruptedException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(org.apache.activemq.artemis.api.core.Message.HDR_DUPLICATE_DETECTION_ID.toString(),
                DUPLICATE_DETECTION_ID);
        routeMock.setAssertPeriod(5000);
        routeMock.expectedMessageCount(3); // two pub and one sub
        template.sendBodyAndHeaders("direct:pub.topic", "demo jms message", headers);
        template.sendBodyAndHeaders("direct:pub.topic", "demo jms message", headers);
        routeMock.assertIsSatisfied();
    }



    @Configuration
    static class ConfigurationTestConfiguration extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:end").log("end");

            from("direct:avro").marshal().avro(Message.class.getName());

            from("direct:pub.topic")
                    .to("publish:topic:pub.topic")
                    .to("direct:end");

            from("subscribe:topic:pub.topic?subscriptionName=test")
                    .to("direct:end");
        }
    }
}
