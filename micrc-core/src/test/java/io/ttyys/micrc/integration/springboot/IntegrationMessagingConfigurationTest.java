package io.ttyys.micrc.integration.springboot;

import com.zaxxer.hikari.HikariDataSource;
import io.ttyys.micrc.integration.EnableMessagingIntegration;
import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration;
import io.ttyys.micrc.integration.springboot.fixtures.Message;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.*;
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
import org.springframework.transaction.annotation.Transactional;

import javax.jms.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
@MockEndpoints("direct:end*")
@SpringBootApplication
@EnableMessagingIntegration(basePackages = "io.ttyys.micrc.integration")
public class IntegrationMessagingConfigurationTest {
    @Autowired
    private CamelContext context;
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
    public void resetMock() throws Exception {
        routeMock.reset();
        mockEndpointSync.reset();
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL + "-0",
                true,
                a -> a.weaveAddLast().to("direct:end-sync").id("direct:end-sync"));
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-0",
                true,
                a -> a.weaveAddLast().to("direct:end").id("direct:end"));
    }

    @AfterEach
    public void clearAdvice() throws Exception {
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL + "-0",
                true,
                a -> a.weaveById("direct:end-sync").remove());
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-0",
                true,
                a -> a.weaveById("direct:end").remove());
    }

    @Test
    public void testBasicRoute() throws InterruptedException {
        routeMock.expectedMessageCount(1);
        routeMock.expectedBodiesReceived("Hello");
        template.sendBody("direct:start", "Hello");
        routeMock.assertIsSatisfied();
    }

    @Test
    public void testBasicSubscribe() throws Exception {
        routeMock.expectedMessageCount(2);
        routeMock.expectedBodiesReceivedInAnyOrder("demo jms message", "demo test");
        template.sendBody("publish:topic:demo.test.topic", "demo jms message");
        routeMock.assertIsSatisfied();
    }

    @SuppressWarnings("unused")
    @EndpointInject("mock:direct:end-sync")
    private MockEndpoint mockEndpointSync;

    @Test
    public void testBasicPublish() throws Exception {
        routeMock.expectedMessageCount(3);
        mockEndpointSync.expectedBodiesReceived("demo jms message");
        routeMock.expectedBodiesReceivedInAnyOrder("demo jms message", "demo test");
        template.sendBody("direct:module1.demo.test.topic", "demo jms message");
        mockEndpointSync.assertIsSatisfied();
        routeMock.assertIsSatisfied();
    }

    @Test
    public void testTmplRouteSubscribe() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBeanMethodName", "demo(io.ttyys.micrc.integration.springboot.fixtures.Message)");
        headers.put("AvroSchemaClassName", "io.ttyys.micrc.integration.springboot.fixtures.Message");
        byte[] o1 = template.requestBody("direct:avro",
                Message.newBuilder().setFrom("test from").setBody("test body").setTo("test to").build(), byte[].class);

        routeMock.expectedMessageCount(1);
        routeMock.expectedBodiesReceived("demo bean test");
        template.sendBodyAndHeaders(
                "publish:topic:test.without.db_tx.topic",
                o1,
                headers);
        routeMock.assertIsSatisfied();
    }

    @Autowired
    private DemoMessageAdapter adapter;

    @Test
    public void testTmplRoutePublishInternal() throws Exception {
        routeMock.setAssertPeriod(5000);
        mockEndpointSync.expectedMessageCount(1);
        routeMock.expectedMessageCount(2);
        adapter.publishInternal(
                Message.newBuilder().setFrom("test from").setBody("test body").setTo("test to").build(),
                "io.ttyys.micrc.integration.springboot.fixtures.Message");
        mockEndpointSync.assertIsSatisfied();
        routeMock.assertIsSatisfied();
    }
}

@Configuration
@Import(DataSourceAutoConfiguration.class)
class ConfigurationTestConfiguration extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:end").log("end");
        from("direct:end-sync").log("end-sync");

        from("direct:start")
                .to("direct:end");

        from("direct:avro").marshal().avro("io.ttyys.micrc.integration.springboot.fixtures.Message");

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
//                TemplatedRouteBuilder
//                        .builder(camelContext,
//                                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION)
//                        .parameter("topicName", "test.without.db_tx.topic")
//                        // clientId和subscriptionName共同唯一确定一个broker持久化队列，服务重启后，使用这个队列继续获取消息
//                        // 确保同一个订阅，这两个属性是稳定不变的，否则会丢失消息。服务升级也要确保消费完所有队列的消息才能停机
//                        // subscriptionName必须以业务服务包名前缀保持全局唯一
//                        .parameter("subscriptionName", "test.sub")
//                        .parameter("adapterName", "demoMessageAdapter")
//                        .parameter("end", "direct:end")
//                        .add();
//
//                TemplatedRouteBuilder
//                        .builder(camelContext,
//                                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL)
//                        .parameter("messagePublishEndpoint", "demo.test.topic")
//                        .parameter("end", "direct:end-sync")
//                        .add();
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

@SuppressWarnings("unused")
@Component
class DemoMessageAdapter {

    @Produce("direct:demo.test.topic")
    private PublishInternal producer;

    public void hello() {
        System.out.println("hello world");
    }
    public String demo(String msg) {
        System.out.println("demo test: " + msg);
        return "demo test";
    }
    public String demo(byte[] msg) {
        System.out.println("demo test: " + msg.length);
        return "demo test";
    }
    public String demo(Message msg) {
        System.out.println("demo bean test: " + msg);
        return "demo bean test";
    }

    @Transactional
    public void publishInternal(Message msg, String clz) {
        producer.sendMsg(msg, clz);
    }
}

interface PublishInternal {
    void sendMsg(@Body Message message, @Header("AvroSchemaClassName") String avroSchemaClassName);
}
