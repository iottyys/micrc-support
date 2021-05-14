package io.ttyys.micrc.integration.route;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.RouteTemplateParameterSource;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.MySqlChannelMessageStoreQueryProvider;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class IntegrationMessagingRouteConfiguration extends RouteBuilder {

    private static final String BUFFERED_MESSAGE_STORE_CHANNEL_PREFIX = "bufferedOutputMessageChannel.";
    private static final String MESSAGE_STORE_PRODUCER_GROUP_PREFIX = "bufferedOutputMessageChannel.";

    public static final String ROUTE_TMPL_MESSAGE_SUBSCRIPTION =
            IntegrationMessagingRouteConfiguration.class.getName() + ".messageSubscription";
    public static final String ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL =
            IntegrationMessagingRouteConfiguration.class.getName() + ".messagePublishInternal";

    @Override
    public void configure() {
        routeTemplate(ROUTE_TMPL_MESSAGE_SUBSCRIPTION)
                .templateParameter("topicName", null, "topic name for subscribe")
                .templateParameter("subscriptionName", null, "subscription-name is required for durable subscription")
                .templateParameter("adapterName", null, "bean name of message handler adapter")
//                .templateParameter("end",
//                        "log:" + IntegrationMessagingRouteConfiguration.class.getName()
//                                + "?showAll=true&multiline=true&level=DEBUG", "endpoint of end")
                .from("subscribe:topic:{{topicName}}?subscriptionName={{subscriptionName}}")
                .toD("dataformat:avro:unmarshal?instanceClassName=${header.AvroSchemaClassName}")
                .to("bean:{{adapterName}}")
                .to("log:" + IntegrationMessagingRouteConfiguration.class.getName()
                        + "?showAll=true&multiline=true&level=DEBUG");

        routeTemplate(ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL)
                .templateParameter("messagePublishEndpoint",
                        null, "endpoint of message publishing")
//                .templateParameter("end",
//                        "log:" + IntegrationMessagingRouteConfiguration.class.getName()
//                                + "?showAll=true&multiline=true&level=DEBUG", "endpoint of end")
                .from("direct:{{messagePublishEndpoint}}")
                .process(exchange -> {
                    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
                        throw new IllegalStateException(
                                "Message publishing must done in transaction. Please open it first. ");
                    }
                })
                .setExchangePattern(ExchangePattern.InOnly)
                .setHeader("CamelJmsDestinationName", constant("{{messagePublishEndpoint}}"))
                .toD("dataformat:avro:marshal?instanceClassName=${header.AvroSchemaClassName}")
                .toD("spring-integration:bufferedOutputMessageChannel.${header.CamelJmsDestinationName}?inOut=false")
                .to("log:" + IntegrationMessagingRouteConfiguration.class.getName()
                        + "?showAll=true&multiline=true&level=DEBUG");

        from("spring-integration:outboundMessageChannel")
                .routeId("io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.outboundMessageChannel")
                .process(exchange -> exchange.getIn().setHeader("CamelJmsDestinationName",
                        exchange.getIn().getHeader("CamelJmsDestinationName")))
                .to("publish:topic:dummyTopic")
                .to("log:" + IntegrationMessagingRouteConfiguration.class.getName()
                        + "?showAll=true&multiline=true&level=DEBUG");
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationMessagingRouteTemplateParameterSource integrationMessagingRouteTemplateParameterSource() {
        return new IntegrationMessagingRouteTemplateParameterSource();
    }

    @Bean("jdbcChannelMessageStore")
    @ConditionalOnMissingBean
    public JdbcChannelMessageStore jdbcChannelMessageStore(DataSource dataSource) {
        JdbcChannelMessageStore store = new JdbcChannelMessageStore();
        store.setDataSource(dataSource);
        store.setChannelMessageStoreQueryProvider(new MySqlChannelMessageStoreQueryProvider());
        store.setPriorityEnabled(true);
        return store;
    }

    @Bean
    public MessageChannel outboundMessageChannel() {
        return MessageChannels.direct().get();
    }

    @Bean("io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.contextConfiguration")
    @Order(LOWEST)
    CamelContextConfiguration contextConfiguration(IntegrationFlowContext context,
                                                   DefaultListableBeanFactory beanFactory,
                                                   JdbcChannelMessageStore jdbcChannelMessageStore,
                                                   ChainedTransactionManager outboundChainedTxManager,
                                                   IntegrationMessagingRouteTemplateParameterSource source) {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                camelContext.getRegistry().bind("IntegrationMessagingRouteTemplateParametersSource",
                        RouteTemplateParameterSource.class, source);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                for (String routeId : source.routeIds()) {
                    Map<String, Object> definition = source.parameters(routeId);
                    Object topicName = definition.get("messagePublishEndpoint");
                    if (topicName instanceof String && StringUtils.hasText(topicName.toString())) {
                        registerMessageStoreIntegrationFlow(context, beanFactory, jdbcChannelMessageStore,
                                outboundChainedTxManager, topicName.toString());
                    }
                }

            }
        };
    }

    private void registerMessageStoreIntegrationFlow(IntegrationFlowContext context,
                                                     DefaultListableBeanFactory beanFactory,
                                                     JdbcChannelMessageStore jdbcChannelMessageStore,
                                                     ChainedTransactionManager outboundChainedTxManager,
                                                     String topicName) {
        MessageChannel channel = MessageChannels
                .priority(BUFFERED_MESSAGE_STORE_CHANNEL_PREFIX + topicName)
                .messageStore(jdbcChannelMessageStore, MESSAGE_STORE_PRODUCER_GROUP_PREFIX + topicName)
                .get();
        //noinspection unchecked
        BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(
                        (Class<MessageChannel>) channel.getClass(),
                        () -> channel)
                .getRawBeanDefinition();
        beanFactory.registerBeanDefinition(BUFFERED_MESSAGE_STORE_CHANNEL_PREFIX + topicName ,beanDefinition);
        IntegrationFlow flow = IntegrationFlows
                .from(BUFFERED_MESSAGE_STORE_CHANNEL_PREFIX + topicName)
                .bridge(e -> e.poller(Pollers.fixedDelay(5000)
                        .maxMessagesPerPoll(-1)).transactional(outboundChainedTxManager, true))
                .channel(outboundMessageChannel())
                .get();
        context.registration(flow).register();
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
