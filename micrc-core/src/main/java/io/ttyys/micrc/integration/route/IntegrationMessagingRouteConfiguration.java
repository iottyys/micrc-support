package io.ttyys.micrc.integration.route;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.UUID;

public class IntegrationMessagingRouteConfiguration extends RouteBuilder {

    public static final String ROUTE_TMPL_MESSAGE_SUBSCRIPTION =
            IntegrationMessagingRouteConfiguration.class.getName() + ".messageSubscription";
    public static final String ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL =
            IntegrationMessagingRouteConfiguration.class.getName() + ".messagePublishInternal";

    @Override
    public void configure() {
        routeTemplate(ROUTE_TMPL_MESSAGE_SUBSCRIPTION)
                .templateParameter("topicName", null, "topic name for subscribe")
                .templateParameter("subscriptionName", null,
                        "subscription-name is required for durable subscription")
                .templateParameter("adapterName", null, "bean name of message handler adapter")
                .from("subscribe:topic:{{topicName}}?subscriptionName={{subscriptionName}}")
                .toD("dataformat:avro:unmarshal?instanceClassName=${header.AvroSchemaClassName}")
                .to("bean:{{adapterName}}")
                .to("log:" + IntegrationMessagingRouteConfiguration.class.getName()
                        + "?showAll=true&multiline=true&level=DEBUG");

        routeTemplate(ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL)
                .templateParameter("messagePublishEndpoint",
                        null, "endpoint of message publishing")
                .from("direct:{{messagePublishEndpoint}}")
                .process(exchange -> {
                    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
                        throw new IllegalStateException(
                                "Message publishing must done in transaction. Please open it first. ");
                    }
                    exchange.getIn().setHeader(Message.HDR_DUPLICATE_DETECTION_ID.toString(),
                            StringUtils.replace(UUID.randomUUID().toString().toUpperCase(), "-", ""));
                })
                .setExchangePattern(ExchangePattern.InOnly)
                .setHeader("CamelJmsDestinationName", constant("{{messagePublishEndpoint}}"))
                .toD("dataformat:avro:marshal?instanceClassName=${header.AvroSchemaClassName}")
                .toD("spring-integration:bufferedOutputMessageChannel.${header.CamelJmsDestinationName}?inOut=false")
                .to("log:" + IntegrationMessagingRouteConfiguration.class.getName()
                        + "?showAll=true&multiline=true&level=DEBUG");

        from("spring-integration:outboundMessageChannel")
                .routeId("io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.outboundMessageChannel")
                .setExchangePattern(ExchangePattern.InOnly)
                .process(exchange -> exchange.getIn().setHeaders(exchange.getIn().getHeaders()))
                .to("publish:topic:dummyTopic")
                .to("log:" + IntegrationMessagingRouteConfiguration.class.getName()
                        + "?showAll=true&multiline=true&level=DEBUG");
    }

    @Data
    @SuperBuilder
    public static abstract class AbstractIntegrationMessagingDefinition {
        protected String templateId;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder
    public static class IntegrationMessagingProducerDefinition extends AbstractIntegrationMessagingDefinition {
        private String messagePublishEndpoint;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder
    public static class IntegrationMessagingConsumerDefinition extends AbstractIntegrationMessagingDefinition {
        private String topicName;
        private String subscriptionName;
        private String adapterName;
    }
}
