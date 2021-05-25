package io.ttyys.micrc.integration.route.fixtures;

import io.ttyys.micrc.annotations.technology.integration.MessageConsumer;
import io.ttyys.micrc.annotations.technology.integration.MessageProducer;
import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Produce;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@Component
public class DemoTxMessageAdapter {

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

    @Transactional
    public void sendMsg100Only() {
        for (int i = 0; i < 100; i++) {
            producer.sendMsg(
                    Message.newBuilder().setTo("to " + i).setFrom("from " + i).setBody("body " + i).build(),
                    Message.class.getName());
        }
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

    @SuppressWarnings("unused")
    @MessageConsumer(id = "test", topicName = "test.tx.topic", subscriptionName = "test.sub", adapterName = "demoTxMessageAdapter")
    public interface SimulatingConsumer {}

    @MessageProducer(id = "test", messagePublishEndpoint = "demo.test.topic")
    public interface SimulatingProducer {
        void sendMsg(@Body Message message, @Header("AvroSchemaClassName") String schemaClassName);
    }
}
