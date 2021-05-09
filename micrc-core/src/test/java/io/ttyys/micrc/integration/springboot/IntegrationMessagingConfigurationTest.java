package io.ttyys.micrc.integration.springboot;

import io.ttyys.micrc.integration.EnableMessagingIntegration;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.MySqlChannelMessageStoreQueryProvider;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.messaging.MessageChannel;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
@MockEndpoints("direct:end")
@SpringBootApplication
@EnableMessagingIntegration
public class IntegrationMessagingConfigurationTest {
    @Autowired
    private JpaTransactionManager jpaTransactionManager;
    @Autowired
    private EmbeddedActiveMQ server;
    @Autowired
    private ConnectionFactory inboundConnectionFactory;
    @Autowired
    private ConnectionFactory outboundConnectionFactory;
    @Autowired
    private JmsTransactionManager inboundTransactionManager;
    @Autowired
    private JmsTransactionManager outboundTransactionManager;
    @Autowired
    private SpringTransactionPolicy INBOUND_TX_PROPAGATION_REQUIRED;
    @Autowired
    private SpringTransactionPolicy OUTBOUND_TX_PROPAGATION_REQUIRED;
    @Autowired
    private JmsComponent publish;
    @Autowired
    private JmsComponent subscribe;

    @Test
    public void testConfiguration() {
        // jpa or other jdbc-based transaction manager
        assertThat(jpaTransactionManager).isNotNull();
        assertThat(server).isNotNull();
        assertThat(server.getActiveMQServer().getConfiguration().getJournalDirectory())
                .isEqualTo("");
        // connection factory
        assertThat(inboundConnectionFactory).isNotNull();
        assertThat(outboundConnectionFactory).isNotNull();
        assertThat(inboundConnectionFactory).isNotEqualTo(outboundConnectionFactory);
        // jms transaction manager
        assertThat(inboundTransactionManager).isNotNull();
        assertThat(outboundTransactionManager).isNotNull();
        assertThat(inboundTransactionManager).isNotEqualTo(outboundTransactionManager);
        // tx manager policy
        assertThat(INBOUND_TX_PROPAGATION_REQUIRED).isNotNull();
        assertThat(OUTBOUND_TX_PROPAGATION_REQUIRED).isNotNull();
        assertThat(INBOUND_TX_PROPAGATION_REQUIRED).isNotEqualTo(OUTBOUND_TX_PROPAGATION_REQUIRED);
        // camel jms component
        assertThat(publish).isNotNull();
        assertThat(subscribe).isNotNull();
        assertThat(publish).isNotEqualTo(subscribe);
    }

    @Autowired
    private ProducerTemplate template;

    @EndpointInject("mock:direct:end")
    private MockEndpoint routeMock;

    @BeforeEach
    public void resetMock() {
        routeMock.reset();
    }

    @Test
    public void testBasicRoute() throws InterruptedException {
        routeMock.expectedMessageCount(1);
        routeMock.expectedBodiesReceived("Hello");
        template.sendBody("direct:start", "Hello");
        routeMock.assertIsSatisfied();
    }

    @Test
    public void testBasicSubscribe() throws InterruptedException {
        routeMock.expectedMessageCount(2);
        routeMock.expectedBodiesReceived("demo jms message");
        template.sendBody("publish:topic:demo.test.topic", "demo jms message");
        routeMock.assertIsSatisfied();
    }

    @Test
    public void testBasicPublish() throws InterruptedException {
        routeMock.expectedMessageCount(2);
        routeMock.expectedBodiesReceived("demo jms message", "demo test");
        template.sendBody("direct:module1.demo.test.topic", "demo jms message");
        routeMock.assertIsSatisfied();
    }

    @Configuration
    static class CamelTestRoute extends RouteBuilder {

        @Override
        public void configure() {
            from("direct:end").log("end");

            from("direct:start")
                    .to("direct:end");

            from("subscribe:topic:demo.test.topic?subscriptionName=demo.test.topic.module1")
                    .to("bean:demoMessageAdapter?method=hello")
                    .to("direct:end");

            from("subscribe:topic:demo.test.topic?subscriptionName=demo.test.topic.module2")
                    .to("bean:demoMessageAdapter?method=demo")
                    .to("direct:end");

            from("direct:module1.demo.test.topic")
                    .to("publish:topic:demo.test.topic");
        }

        @Bean
        CamelContextConfiguration contextConfiguration() {
            return new CamelContextConfiguration() {
                @Override
                public void beforeApplicationStart(CamelContext camelContext) {
//                    TemplatedRouteBuilder.builder(camelContext, "myTemplate")
//                            .parameter("name", "one")
//                            .parameter("greeting", "Hello")
//                            .add();
                }

                @Override
                public void afterApplicationStart(CamelContext camelContext) {

                }
            };
        }
    }
}

@Component
class DemoMessageAdapter {
    public void hello() {
        System.out.println("hello world");
    }
    public String demo(String msg) {
        System.out.println("demo test: " + msg);
        return "demo test";
    }
}
