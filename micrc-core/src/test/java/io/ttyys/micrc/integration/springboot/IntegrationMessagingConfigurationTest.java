package io.ttyys.micrc.integration.springboot;

import com.zaxxer.hikari.HikariDataSource;
import io.ttyys.micrc.integration.EnableMessagingIntegration;
import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.H2ChannelMessageStoreQueryProvider;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;

import javax.jms.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
@MockEndpoints("direct:end*")
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
    private JmsTransactionManager outboundTransactionManager;
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
        assertThat(outboundTransactionManager).isNotNull();
        // camel jms component
        assertThat(publish).isNotNull();
        assertThat(subscribe).isNotNull();
        assertThat(publish).isNotEqualTo(subscribe);
    }

    @Autowired
    private ProducerTemplate template;

    @SuppressWarnings("unused")
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
        routeMock.expectedBodiesReceivedInAnyOrder("demo jms message", "demo test");
        template.sendBody("publish:topic:demo.test.topic", "demo jms message");
        routeMock.assertIsSatisfied();
    }

    @SuppressWarnings("unused")
    @EndpointInject("mock:direct:end-sync")
    private MockEndpoint mockEndpointSync;

    @Test
    public void testBasicPublish() throws InterruptedException {
        routeMock.expectedMessageCount(3);
        mockEndpointSync.expectedBodiesReceived("demo jms message");
        routeMock.expectedBodiesReceivedInAnyOrder("demo jms message", "demo test");
        template.sendBody("direct:module1.demo.test.topic", "demo jms message");
        mockEndpointSync.assertIsSatisfied();
        routeMock.assertIsSatisfied();
    }

    @Test
    public void testTmplRouteSubscribe() throws InterruptedException {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBeanMethodName", "demo(String)");
        routeMock.expectedMessageCount(1);
        routeMock.expectedBodiesReceived("demo test");
        template.sendBodyAndHeaders(
                "publish:topic:test.without.db_tx.topic",
                "tmpl route subscribe jms message",
                headers);
        routeMock.assertIsSatisfied();
    }

    @Test
    public void testTmplRoutePublishInternal() throws InterruptedException {
        routeMock.expectedMessageCount(3);
        template.requestBody(
                "direct:demo.test.topic",
                "tmpl route publish jms message to message store");
        routeMock.assertIsSatisfied();
    }

    @Configuration
    @Import(DataSourceAutoConfiguration.class)
    static class CamelTestRoute extends RouteBuilder {

        @Override
        public void configure() {
            from("direct:end").log("end");
            from("direct:end-sync").log("end-sync");

            from("direct:start")
                    .to("direct:end");

            from("subscribe:topic:demo.test.topic?subscriptionName=demo.test.topic.module1")
                    .to("bean:demoMessageAdapter?method=hello")
                    .to("direct:end");

            from("subscribe:topic:demo.test.topic?subscriptionName=demo.test.topic.module2")
                    .to("bean:demoMessageAdapter?method=demo")
                    .to("direct:end");

            from("direct:module1.demo.test.topic")
                    .to("publish:topic:demo.test.topic")
                    .to("direct:end-sync");
        }

        @Bean
        CamelContextConfiguration contextConfiguration() {
            return new CamelContextConfiguration() {
                @Override
                public void beforeApplicationStart(CamelContext camelContext) {
                    TemplatedRouteBuilder
                            .builder(camelContext,
                                    IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION)
                            .parameter("topicName", "test.without.db_tx.topic")
                            // clientId和subscriptionName共同唯一确定一个broker持久化队列，服务重启后，使用这个队列继续获取消息
                            // 确保同一个订阅，这两个属性是稳定不变的，否则会丢失消息。服务升级也要确保消费完所有队列的消息才能停机
                            // subscriptionName必须以业务服务包名前缀保持全局唯一
                            .parameter("subscriptionName", "test.sub")
                            .parameter("adapterName", "demoMessageAdapter")
                            .parameter("end", "mock:direct:end")
                            .add();

                    TemplatedRouteBuilder
                            .builder(camelContext,
                                    IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL)
                            .parameter("messagePublishEndpoint", "demo.test.topic")
                            .parameter("end", "direct:end")
                            .add();
                }

                @Override
                public void afterApplicationStart(CamelContext camelContext) {
                }
            };
        }

        @Bean
        public JdbcChannelMessageStore jdbcChannelMessageStore(HikariDataSource dataSource) {
            JdbcChannelMessageStore store = new JdbcChannelMessageStore();
            store.setDataSource(dataSource);
            store.setChannelMessageStoreQueryProvider(new H2ChannelMessageStoreQueryProvider());
            store.setPriorityEnabled(true);
            return store;
        }
    }
}

@SuppressWarnings("unused")
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
