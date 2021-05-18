package io.ttyys.micrc.integration.springboot.fixtures;

import io.ttyys.micrc.annotations.technology.integration.MessageConsumer;
import io.ttyys.micrc.annotations.technology.integration.MessageProducer;
import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.Produce;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unused")
@Component
public class DemoMessageAdapter {

    @Produce("direct:demo.test.topic")
    private SimulatingProducer producer;

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

    @SuppressWarnings("unused")
    @MessageConsumer(topicName = "test.without.db_tx.topic", subscriptionName = "test.sub", adapterName = "demoMessageAdapter")
    public interface SimulatingConsumer {}

    @MessageProducer(messagePublishEndpoint = "demo.test.topic")
    public interface SimulatingProducer {
        void sendMsg(@Body Message message, @Header("AvroSchemaClassName") String avroSchemaClassName);
    }
}
