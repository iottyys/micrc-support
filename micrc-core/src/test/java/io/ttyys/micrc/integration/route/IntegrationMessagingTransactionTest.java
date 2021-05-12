package io.ttyys.micrc.integration.route;

import com.zaxxer.hikari.HikariDataSource;
import io.ttyys.micrc.integration.EnableMessagingIntegration;
import io.ttyys.micrc.integration.springboot.fixtures.Message;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientRequestor;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.management.ManagementHelper;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQSession;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.H2ChannelMessageStoreQueryProvider;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@MockEndpoints("direct:end")
@SpringBootApplication
@EnableMessagingIntegration
public class IntegrationMessagingTransactionTest {

    @Autowired
    private CamelContext context;

    @Autowired
    private ProducerTemplate template;

    @SuppressWarnings("unused")
    @EndpointInject("mock:direct:end")
    private MockEndpoint mock;

    @BeforeEach
    public void resetMock() {
        mock.reset();
    }

    @Test
    @Order(1)
    public void testSubscribeWithAllSubmitToSuccessConsistent() throws InterruptedException {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBeanMethodName",
                "fakeTxOp(org.apache.camel.Exchange)");
        headers.put("AvroSchemaClassName",
                "io.ttyys.micrc.integration.springboot.fixtures.Message");
        mock.setAssertPeriod(5000);
        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived("result", 0L);
        byte[] o1 = template.requestBody("direct:avro",
                Message.newBuilder().setFrom("test from").setBody("test body").setTo("test to").build(), byte[].class);
        template.sendBodyAndHeaders("publish:topic:test.tx.topic", o1, headers);
        mock.assertIsSatisfied();
    }

    @Test
    @Order(2)
    public void testSubscribeWithDbRollbackToFailConsistent() throws Exception {
        AdviceWith.adviceWith(context,
                "test-consumer",
                a -> a.weaveAddFirst().onException(RuntimeException.class).handled(false).to("seda:end"));
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBeanMethodName", "fakeTxRbOp(org.apache.camel.Exchange)");
        headers.put("AvroSchemaClassName", "io.ttyys.micrc.integration.springboot.fixtures.Message");
        mock.setAssertPeriod(5000);
        mock.expectedMinimumMessageCount(1);
        mock.expectedHeaderReceived("result", 1L);
        byte[] o1 = template.requestBody("direct:avro",
                Message.newBuilder().setFrom("test from").setBody("test body").setTo("test to").build(), byte[].class);
        template.sendBodyAndHeaders("publish:topic:test.tx.topic", o1, headers);
        mock.assertIsSatisfied();
    }

    @Test
    @Order(3)
    public void testSubscribeWithJmsRollbackToDuplication() throws Exception {
        AdviceWith.adviceWith(context, "test-consumer", a -> a.weaveAddLast().process(exchange -> {
            throw new Exception("simulating message ack error after tx of db commit. ");
        }));
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBeanMethodName",
                "fakeTxOp(org.apache.camel.Exchange)");
        headers.put("AvroSchemaClassName",
                "io.ttyys.micrc.integration.springboot.fixtures.Message");
        mock.setAssertPeriod(5000);
        mock.expectedMinimumMessageCount(1);
        mock.expectedHeaderReceived("result", 2L);
        byte[] o1 = template.requestBody("direct:avro",
                Message.newBuilder().setFrom("test from").setBody("test body").setTo("test to").build(), byte[].class);
        template.sendBodyAndHeaders("publish:topic:test.tx.topic", o1, headers);
        mock.assertIsSatisfied();
    }
}

@Configuration
@Import(DataSourceAutoConfiguration.class)
class ConfigurationTestConfiguration extends RouteBuilder {

    @Override
    public void configure() {
        // retrieve message count for check tx submit or rollback
        from("seda:end").delay(3000).process(exchange -> {
            ActiveMQConnection connection = exchange.getProperty("connection", ActiveMQConnection.class);
            ActiveMQSession session = (ActiveMQSession) connection.createSession();
            ClientSession clientSession = session.getCoreSession();
            ClientRequestor requestor = new ClientRequestor(clientSession, "activemq.management");
            ClientMessage message = clientSession.createMessage(false);
            ManagementHelper.putAttribute(message, ResourceNames.QUEUE + ".test\\.sub", "durableMessageCount");
            session.start();
            ClientMessage reply = requestor.request(message);
            Long count = (Long) ManagementHelper.getResult(reply);
            ManagementHelper.putAttribute(message, ResourceNames.QUEUE + "DLQ", "durableMessageCount");
            ClientMessage replyDLQ = requestor.request(message);
            Long countDLQ = (Long) ManagementHelper.getResult(replyDLQ);
            exchange.getIn().setHeader("result", count + countDLQ);
            requestor.close();
            session.close();
            connection.close();
        }).to("direct:end").log("async end");

        from("direct:end").log("sync end");
        from("direct:end-sync").log("end-sync");

        from("direct:start")
                .to("direct:end");

        from("direct:avro").marshal().avro("io.ttyys.micrc.integration.springboot.fixtures.Message");
    }

    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                TemplatedRouteBuilder
                        .builder(camelContext,
                                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION)
                        .routeId("test-consumer")
                        .parameter("topicName", "test.tx.topic")
                        // clientId和subscriptionName共同唯一确定一个broker持久化队列，服务重启后，使用这个队列继续获取消息
                        // 确保同一个订阅，这两个属性是稳定不变的，否则会丢失消息。服务升级也要确保消费完所有队列的消息才能停机
                        // subscriptionName必须以业务服务包名前缀保持全局唯一
                        .parameter("subscriptionName", "test.sub")
                        .parameter("adapterName", "demoTxMessageAdapter")
                        .parameter("end", "seda:end")
                        .add();

                TemplatedRouteBuilder
                        .builder(camelContext,
                                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL)
                        .routeId("test-producer")
                        .parameter("messagePublishEndpoint", "demo.test.topic")
                        .parameter("end", "direct:end-sync")
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

@SuppressWarnings("unused")
@Component
class DemoTxMessageAdapter {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Transactional
    public void fakeTxOp(Exchange exchange) throws Exception {
        this.setConnection(exchange);
        LoggerFactory.getLogger(DemoTxMessageAdapter.class).debug("==============fake tx op");
    }

    @Transactional
    public void fakeTxRbOp(Exchange exchange) throws Exception {
        this.setConnection(exchange);
        throw new RuntimeException("simulating exception for rollback transaction. ");
    }

    private void setConnection(Exchange exchange) throws Exception {
        Map<Object, Object> resourceMap = TransactionSynchronizationManager.getResourceMap();
        for (Map.Entry<Object, Object> entry : resourceMap.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof CachingConnectionFactory) {
                ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) ((CachingConnectionFactory) key).getTargetConnectionFactory();
                assert factory != null;
                ActiveMQConnection connection = (ActiveMQConnection) factory.createConnection();
                exchange.setProperty("connection", connection);
                return;
            }
        }
    }
}
