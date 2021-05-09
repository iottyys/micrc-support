package io.ttyys.micrc.integration.route;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.MySqlChannelMessageStoreQueryProvider;
import org.springframework.messaging.MessageChannel;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.sql.DataSource;

@Configuration
@Import(DataSourceAutoConfiguration.class)
public class IntegrationMessagingRouteConfiguration extends RouteBuilder {
    @Override
    public void configure() {
        routeTemplate("io.ttyys.micrc.integration.route.IntegrationMessagingRouteBuilder.messageSubscription")
                .templateParameter("topicName", null, "topic name for subscribe")
                .templateParameter("subscriptionName", null, "jms 2.0 required for durable subscription")
                .templateParameter("adapterName", null, "bean name of message handler adapter")
                .from("subscribe:topic:{{topicName}}?subscriptionName={{subscriptionName}}")
                .transacted("INBOUND_TX_PROPAGATION_REQUIRED")
                .unmarshal().avro(simple("${header.AvroMessageClassName}"))
                .toD("bean:{{adapterName}}")
                .end();

        routeTemplate("io.ttyys.micrc.integration.route.IntegrationMessagingRouteBuilder.messagePublishInternal")
                .templateParameter("messagePublishEndpoint", null, "endpoint of message publishing")
                .from("direct:{{messagePublishEndpoint}}")
                .transacted("DATABASE_TRANSACTION_PROPAGATION_REQUIRED")
                .marshal().avro(simple("${header.AvroMessageClassName}"))
                .setExchangePattern(ExchangePattern.InOnly)
                .to("spring-integration:bufferedOutputMessageChannel?inOut=false")
                .setExchangePattern(ExchangePattern.InOut)
                .end();

        routeTemplate("io.ttyys.micrc.integration.route.IntegrationMessagingRouteBuilder.messagePublishExternal")
                .templateParameter("topicName", null, "topic name for publish")
                .from("spring-integration:outboundMessageChannel")
                .transacted("OUTBOUND_TX_PROPAGATION_REQUIRED")
                .to("publish:topic:{{topicName}}")
                .end();
    }

    @Bean
    public JdbcChannelMessageStore jdbcChannelMessageStore(@Qualifier("dataSource") DataSource dataSource) {
        JdbcChannelMessageStore store = new JdbcChannelMessageStore();
        store.setDataSource(dataSource);
        store.setChannelMessageStoreQueryProvider(new MySqlChannelMessageStoreQueryProvider());
        return store;
    }

    @Bean
    public MessageChannel bufferedOutputMessageChannel() {
        return MessageChannels.queue("jdbcChannelMessageStore").get();
    }

    @Bean
    public MessageChannel outboundMessageChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public IntegrationFlow outboundMessageBridge(JpaTransactionManager jpaTransactionManager) {
        return IntegrationFlows.from(bufferedOutputMessageChannel())
                .bridge(e -> e.poller(Pollers.fixedDelay(5000)
                        .maxMessagesPerPoll(-1)).transactional(jpaTransactionManager))
                .channel(outboundMessageChannel())
                .get();
    }
}
