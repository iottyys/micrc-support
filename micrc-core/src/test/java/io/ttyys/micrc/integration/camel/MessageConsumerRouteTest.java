package io.ttyys.micrc.integration.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

@CamelSpringBootTest
@SpringBootApplication(scanBasePackages = "io.ttyys.micrc")
public class MessageConsumerRouteTest {

//    @EndpointInject("jms:demo.queue?disableReplyTo=true&deliveryMode=2")
//    private ProducerTemplate queueProducer;
    @EndpointInject("jms:topic:Demo")
    private ProducerTemplate topicProducer;

    @Test
    public void testMessageConsumerRoute() {
//        producer.requestBody("");
//        producer.sendBody("");
//        queueProducer.sendBody("");
        topicProducer.sendBody("");
    }

    @Configuration
    static class MessageConsumerRouteBuilder extends RouteBuilder {

        @Bean
        CamelContextConfiguration contextConfiguration() {
            return new CamelContextConfiguration() {
                @Override
                public void beforeApplicationStart(CamelContext camelContext) {
                    TemplatedRouteBuilder.builder(camelContext, "myTemplate")
                            .parameter("name", "one")
                            .parameter("greeting", "Hello")
                            .add();
                }

                @Override
                public void afterApplicationStart(CamelContext camelContext) {

                }
            };
        }

        @Override
        public void configure() throws Exception {
            // 消费路由
            from("jms:topic:Demo::topic.queue?subscriptionDurable=true").log("test topic durable queue");
//            from("jms:demo.queue?disableReplyTo=true&jmsMessageType=Bytes&testConnectionOnStartup=true&cacheLevelName=CACHE_CONSUMER").log("queue test test");
//            from("jms:topic:Demo::topic.queue?disableReplyTo=true&jmsMessageType=Bytes&testConnectionOnStartup=true&cacheLevelName=CACHE_CONSUMER").log("topic test test");
            // 生产路由
//            from("direct:start").to("spring-integration:message-store");

            // create a route template with the given name
            routeTemplate("myTemplate")
                    // here we define the required input parameters (can have default values)
                    .templateParameter("name")
                    .templateParameter("greeting")
                    .templateParameter("myPeriod", "3s")
                    // here comes the route in the template
                    // notice how we use {{name}} to refer to the template parameters
                    // we can also use {{propertyName}} to refer to property placeholders
                    .from("timer:{{name}}?period={{myPeriod}}")
                    .setBody(simple("{{greeting}} ${body}"))
                    .log("${body}");
        }

        // 消息存储
        @Bean
        public IntegrationFlow myFlow() {
            return IntegrationFlows.from("input")
                    .filter("World"::equals)
                    .transform("Hello "::concat)
                    .handle(System.out::println)
                    .get();
        }

        // 消息发送
    }
}
