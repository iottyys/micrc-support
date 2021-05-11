package io.ttyys.micrc.integration.route;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.MySqlChannelMessageStoreQueryProvider;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;

@Configuration
@Import(DataSourceAutoConfiguration.class)
public class IntegrationMessagingRouteConfiguration extends RouteBuilder {

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
                .templateParameter("end",
                        "log:" + IntegrationMessagingRouteConfiguration.class.getName()
                                + "?showAll=true&multiline=true&level=DEBUG", "endpoint of end")
                .from("subscribe:topic:{{topicName}}?subscriptionName={{subscriptionName}}")
//                .unmarshal().avro(simple("${header.AvroMessageClassName}"))
                .to("bean:{{adapterName}}")
                .to("{{end}}")
                .end();

        routeTemplate(ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL)
                .templateParameter("messagePublishEndpoint", null, "endpoint of message publishing")
                .templateParameter("end",
                        "log:" + IntegrationMessagingRouteConfiguration.class.getName()
                                + "?showAll=true&multiline=true&level=DEBUG", "endpoint of end")
                .from("direct:{{messagePublishEndpoint}}")
                .process(exchange -> {
                    if (TransactionSynchronizationManager.isActualTransactionActive()) {
                        throw new IllegalStateException(
                                "message publishing must done in transaction. please open it first. ");
                    }
                })
//                .marshal().avro(simple("${header.AvroMessageClassName}"))
                .setExchangePattern(ExchangePattern.InOnly)
                .setHeader("CamelJmsDestinationName", constant("{{messagePublishEndpoint}}"))
                .to("spring-integration:bufferedOutputMessageChannel?inOut=false")
                .setExchangePattern(ExchangePattern.InOut)
                .to("{{end}}")
                .end();

        from("spring-integration:outboundMessageChannel")
                .process(exchange -> {
                    if (TransactionSynchronizationManager.isActualTransactionActive()) {
                        throw new IllegalStateException(
                                "message publishing must done in transaction. please open it first. ");
                    }
                })
                .process(exchange -> exchange.getIn().setHeader("CamelJmsDestinationName",
                        exchange.getIn().getHeader("CamelJmsDestinationName")))
                .to("publish:topic:dynamicDest")
                .to("log:" + IntegrationMessagingRouteConfiguration.class.getName()
                        + "?showAll=true&multiline=true&level=DEBUG")
                .end();
    }

    @Bean("jdbcChannelMessageStore")
    @ConditionalOnMissingBean
    public JdbcChannelMessageStore jdbcChannelMessageStore(@Qualifier("dataSource") DataSource dataSource) {
        JdbcChannelMessageStore store = new JdbcChannelMessageStore();
        store.setDataSource(dataSource);
        store.setChannelMessageStoreQueryProvider(new MySqlChannelMessageStoreQueryProvider());
        store.setPriorityEnabled(true);
        return store;
    }

    @Bean
    public MessageChannel bufferedOutputMessageChannel(JdbcChannelMessageStore jdbcChannelMessageStore) {
        return MessageChannels.priority(jdbcChannelMessageStore, "bufferedOutputMessageChannel").get();
    }

    @Bean
    public MessageChannel outboundMessageChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public IntegrationFlow outboundMessageBridge(ChainedTransactionManager outboundChainedTxManager) {
        return IntegrationFlows.from("bufferedOutputMessageChannel")
                .bridge(e -> e.poller(Pollers.fixedDelay(5000)
                        .maxMessagesPerPoll(-1)).transactional(outboundChainedTxManager, true))
                .channel(outboundMessageChannel())
                .get();
    }
}
