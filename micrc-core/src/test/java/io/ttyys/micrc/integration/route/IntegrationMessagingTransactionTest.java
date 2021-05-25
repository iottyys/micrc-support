package io.ttyys.micrc.integration.route;

import foo.bar.app.IntegrationMessagingTestApplication;
import io.ttyys.micrc.integration.route.fixtures.CustomerRepository;
import io.ttyys.micrc.integration.route.fixtures.DemoTxMessageAdapter;
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
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.init.Jackson2RepositoryPopulatorFactoryBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@CamelSpringBootTest
@MockEndpoints("direct:end*")
@SpringBootTest(classes = {
        IntegrationMessagingTransactionTest.ConfigurationTestConfiguration.class,
        IntegrationMessagingTestApplication.TransactionTestApplication.class,
        DemoTxMessageAdapter.class
}, properties = { "logging.level.root=DEBUG" })
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
    public void resetMock() throws Exception {
        mock.reset();
        jdbcTemplate.execute("DELETE FROM INT_CHANNEL_MESSAGE");
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL + "-test",
                true,
                a -> a.weaveAddLast().to("direct:end-sync").id("direct:end-sync"));
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-test",
                true,
                a -> a.weaveAddLast().to("seda:end").id("seda:end"));
    }

    @AfterEach
    public void clearAdvice() throws Exception {
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL + "-test",
                true,
                a -> a.weaveById("direct:end-sync").remove());
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-test",
                true,
                a -> a.weaveById("seda:end").remove());
        try {
            AdviceWith.adviceWith(context,
                    IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-test",
                    true,
                    a -> a.weaveById("exception").selectFirst().remove());
            AdviceWith.adviceWith(context,
                    IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-test",
                    a -> a.weaveById("process:exception").selectLast().remove());
        } catch (Exception ignore) {}
    }

    @Test
    @Order(1)
    public void testSubscribeWithAllSubmitToSuccessConsistent() throws InterruptedException {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBeanMethodName",
                "fakeTxOp(org.apache.camel.Exchange)");
        headers.put("AvroSchemaClassName", Message.class.getName());
        mock.setAssertPeriod(5000);
        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived("result", 0L);
        byte[] o1 = template.requestBody("direct:avro",
                Message.newBuilder().setFrom("test from").setBody("test body").setTo("test to").build(), byte[].class);
        template.sendBodyAndHeaders("publish:topic:test.tx.topic", o1, headers);
        mock.assertIsSatisfied();
    }

    @Test
    public void testSubscribeWithDbRollbackToFailConsistent() throws Exception {
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-test",
                true,
                a -> a.weaveAddFirst().onException(RuntimeException.class).id("exception").handled(false).to("seda:end"));
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBeanMethodName", "fakeTxRbOp(org.apache.camel.Exchange)");
        headers.put("AvroSchemaClassName", Message.class.getName());
        mock.setAssertPeriod(5000);
        mock.expectedMinimumMessageCount(2);
        mock.message(0).header("result").isGreaterThanOrEqualTo(1);
        byte[] o1 = template.requestBody("direct:avro",
                Message.newBuilder().setFrom("test from").setBody("test body").setTo("test to").build(), byte[].class);
        template.sendBodyAndHeaders("publish:topic:test.tx.topic", o1, headers);
        mock.assertIsSatisfied();
    }

    @Test
    public void testSubscribeWithJmsRollbackToDuplication() throws Exception {
        AdviceWith.adviceWith(context,
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-test",
                a -> a.weaveAddLast().process(exchange -> {
                    throw new Exception("simulating message ack error after tx of db commit. ");
                }).id("process:exception"));
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBeanMethodName",
                "fakeTxOp(org.apache.camel.Exchange)");
        headers.put("AvroSchemaClassName", Message.class.getName());
        mock.setAssertPeriod(5000);
        mock.expectedMinimumMessageCount(2); // broker redelivery again and again
        mock.message(0).header("result").isGreaterThanOrEqualTo(1);
        byte[] o1 = template.requestBody("direct:avro",
                Message.newBuilder().setFrom("test from").setBody("test body").setTo("test to").build(), byte[].class);
        template.sendBodyAndHeaders("publish:topic:test.tx.topic", o1, headers);
        mock.assertIsSatisfied();
        List<Exchange> list = mock.getReceivedExchanges();
        assertThat(list.get(0).getIn().getHeader("JMSMessageID"))
                .isEqualTo(list.get(1).getIn().getHeader("JMSMessageID"));
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

    @Configuration
    @Import(DataSourceAutoConfiguration.class)
    static class ConfigurationTestConfiguration extends RouteBuilder {
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

            from("direct:avro").marshal().avro(Message.class.getName());
        }

        @Bean
        public Jackson2RepositoryPopulatorFactoryBean getRepositoryPopulator() {
            Jackson2RepositoryPopulatorFactoryBean factory = new Jackson2RepositoryPopulatorFactoryBean();
            factory.setResources(new Resource[]{new ClassPathResource("customer-data.json")});
            return factory;
        }
    }
}
