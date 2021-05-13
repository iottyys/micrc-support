package io.ttyys.micrc.integration.route;

import com.zaxxer.hikari.HikariDataSource;
import io.ttyys.micrc.integration.EnableMessagingIntegration;
import io.ttyys.micrc.integration.route.fixtures.CustomerRepository;
import io.ttyys.micrc.integration.route.fixtures.Message;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.init.Jackson2RepositoryPopulatorFactoryBean;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.H2ChannelMessageStoreQueryProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
@CamelSpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@MockEndpoints("direct:end")
@SpringBootApplication
@EnableMessagingIntegration
public class IntegrationMessagingTransactionTest {

    @Autowired
    private CamelContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProducerTemplate template;

    @SuppressWarnings("unused")
    @EndpointInject("mock:direct:end")
    private MockEndpoint mock;

    @BeforeEach
    public void resetMock() {
        mock.reset();
        jdbcTemplate.execute("DELETE FROM INT_CHANNEL_MESSAGE");
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

    @Autowired
    private DemoTxMessageAdapter service;
    @Autowired
    private CustomerRepository repository;

    @Test
    public void testBasicProduce() throws InterruptedException {
        mock.setAssertPeriod(1000);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Message.class);
        service.basicProduce();
        mock.assertIsSatisfied();
    }

    @Test
    public void testPublishInternal() {
        assertThat(jdbcTemplate).isNotNull();
        service.fakeServiceSendMsg();
        assertThat(repository.count()).isEqualTo(0);
        Collection<?> result = jdbcTemplate.queryForList("SELECT * FROM INT_CHANNEL_MESSAGE FOR UPDATE");
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    @Test
    public void testPublishJms() throws Exception {
        assertThat(jdbcTemplate).isNotNull();
        AdviceWith.adviceWith(context,
                "io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.outboundMessageChannel",
                a -> a.weaveAddLast().process(exchange -> {
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
                }).to("seda:end-pub"));
        service.fakeServiceSendMsg();
        Collection<?> result = jdbcTemplate.queryForList("SELECT * FROM INT_CHANNEL_MESSAGE FOR UPDATE");
        assertThat(result).isNotNull();
        assertThat(result).hasSizeGreaterThanOrEqualTo(1);
        mock.setAssertPeriod(5000);
        mock.expectedMessageCount(1);
        mock.whenExchangeReceived(1, exchange -> {
            Collection<?> count = jdbcTemplate.queryForList("SELECT * FROM INT_CHANNEL_MESSAGE FOR UPDATE");
            assertThat(count).isNotNull();
            assertThat(count).isEmpty();
        });
        mock.message(1).header("result").isEqualTo(1);
        mock.assertIsSatisfied();
    }
}

@Configuration
@Import(DataSourceAutoConfiguration.class)
class ConfigurationTestConfiguration extends RouteBuilder {

    @Override
    public void configure() {
        // retrieve message count for check tx submit or rollback with subscribe
        from("seda:end").delay(3000).process(exchange -> {
            ActiveMQConnection connection = exchange.getProperty("connection", ActiveMQConnection.class);
            ActiveMQSession session = (ActiveMQSession) connection.createSession();
            ClientSession clientSession = session.getCoreSession();
            ClientRequestor requestor = new ClientRequestor(clientSession, "activemq.management");
            ClientMessage message = clientSession.createMessage(false);
            ManagementHelper.putAttribute(message, ResourceNames.QUEUE + ".test\\.sub", "durableMessageCount");
            clientSession.start();
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

        // retrieve message count for check tx submit or rollback with publish
        from("seda:end-pub").delay(3000).process(exchange -> {
            ActiveMQConnection connection = exchange.getProperty("connection", ActiveMQConnection.class);
            ActiveMQSession session = (ActiveMQSession) connection.createSession();
            ClientSession clientSession = session.getCoreSession();
            ClientRequestor requestor = new ClientRequestor(clientSession, "activemq.management");
            ClientMessage message = clientSession.createMessage(false);
            ManagementHelper.putAttribute(message, ResourceNames.QUEUE + ".test", "durableMessageCount");
            clientSession.start();
            ClientMessage reply = requestor.request(message);
            Long count = (Long) ManagementHelper.getResult(reply);
            ManagementHelper.putAttribute(message, ResourceNames.QUEUE + "DLQ", "durableMessageCount");
            ClientMessage replyDLQ = requestor.request(message);
            Long countDLQ = (Long) ManagementHelper.getResult(replyDLQ);
            exchange.getIn().setHeader("result", count + countDLQ);
            requestor.close();
            session.close();
            connection.close();
        }).to("direct:end").log("async end-pub");

        from("direct:end").log("sync end");
        from("direct:end-sync").log("end-sync");

        from("direct:start")
                .to("direct:end");

        // simulating error remain the message in queue for counting check
        from("subscribe:topic:demo.test.topic?subscriptionName=test").process(exchange -> {
            throw new RuntimeException();
        });

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

    @Bean
    public Jackson2RepositoryPopulatorFactoryBean getRepositoryPopulator() {
        Jackson2RepositoryPopulatorFactoryBean factory = new Jackson2RepositoryPopulatorFactoryBean();
        factory.setResources(new Resource[]{new ClassPathResource("customer-data.json")});
        return factory;
    }
}

@Component
class DemoTxMessageAdapter {

    @SuppressWarnings("unused")
    @Transactional
    public void fakeTxOp(Exchange exchange) throws Exception {
        this.setConnection(exchange);
        LoggerFactory.getLogger(DemoTxMessageAdapter.class).debug("==============fake tx op");
    }

    @SuppressWarnings("unused")
    @Transactional
    public void fakeTxRbOp(Exchange exchange) throws Exception {
        this.setConnection(exchange);
        throw new RuntimeException("simulating exception for rollback transaction. ");
    }

    @SuppressWarnings("unused")
    @Produce("direct:start")
    private SimulatingProducer basicProducer;

    @SuppressWarnings("unused")
    @Produce("direct:demo.test.topic")
    private SimulatingProducer producer;

    @Transactional
    public void basicProduce() {
        basicProducer.sendMsg(
                Message.newBuilder()
                        .setFrom("test produce from")
                        .setTo("test produce to")
                        .setBody("test produce body")
                        .build(), "");
    }

    @Autowired
    private CustomerRepository repository;

    @Transactional
    public void fakeServiceSendMsg() {
        repository.deleteAll();
        producer.sendMsg(
                Message.newBuilder()
                        .setFrom("test produce from")
                        .setTo("test produce to")
                        .setBody("test produce body")
                        .build(), "io.ttyys.micrc.integration.route.fixtures.Message");
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

interface SimulatingProducer {
    void sendMsg(@Body Message message, @Header("AvroSchemaClassName") String schemaClassName);
}
