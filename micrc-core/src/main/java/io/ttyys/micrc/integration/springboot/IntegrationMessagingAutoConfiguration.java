package io.ttyys.micrc.integration.springboot;

import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration;
import io.ttyys.micrc.integration.route.IntegrationMessagingRouteTemplateParameterSource;
import io.ttyys.micrc.persistence.springboot.PersistenceAutoConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsMessageType;
import org.apache.camel.component.jms.springboot.JmsComponentAutoConfiguration;
import org.apache.camel.spi.RouteTemplateParameterSource;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.H2ChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.MySqlChannelMessageStoreQueryProvider;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.messaging.MessageChannel;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Propagation;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAutoConfiguration(exclude = JmsComponentAutoConfiguration.class)
@AutoConfigureBefore(JmsAutoConfiguration.class)
@AutoConfigureAfter({ JndiConnectionFactoryAutoConfiguration.class })
@ConditionalOnClass({ ConnectionFactory.class, ActiveMQConnectionFactory.class })
@ConditionalOnMissingBean(ConnectionFactory.class)
@EnableConfigurationProperties({ ArtemisProperties.class, JmsProperties.class })
@Import({ ArtemisEmbeddedServerConfiguration.class,
        PersistenceAutoConfiguration.class,
        IntegrationMessagingRouteConfiguration.class })
public class IntegrationMessagingAutoConfiguration {
    private final ArtemisProperties artemisProperties;

    @Autowired
    public IntegrationMessagingAutoConfiguration(ArtemisProperties artemisProperties) {
        this.artemisProperties = artemisProperties;
    }

    @Bean("inboundConnectionFactory")
    public CachingConnectionFactory inboundConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(createConnectionFactory());
        // avoid prefetch
        connectionFactory.setCacheConsumers(false);
        // todo 服务负载均衡的集群环境下，持久化订阅的配置
        connectionFactory.setClientId("");
        return connectionFactory;
    }

    @Bean("outboundConnectionFactory")
    public CachingConnectionFactory outboundConnectionFactory() {
        return new CachingConnectionFactory(createConnectionFactory());
    }

    @Bean("outboundTransactionManager")
    public JmsTransactionManager outboundTransactionManager() {
        JmsTransactionManager transactionManager = new JmsTransactionManager();
        transactionManager.setConnectionFactory(outboundConnectionFactory());
        return transactionManager;
    }

    @Bean
    public ChainedTransactionManager outboundChainedTxManager(JpaTransactionManager jpaTransactionManager) {
        return new ChainedTransactionManager(jpaTransactionManager, outboundTransactionManager());
    }

    @Bean("publish")
    public JmsComponent publish() {
        JmsComponent publisher = JmsComponent.jmsComponent(outboundConnectionFactory());
        publisher.setTransactionManager(outboundTransactionManager());
        publisher.setDisableReplyTo(true);
        publisher.setTestConnectionOnStartup(true);
        publisher.setJmsMessageType(JmsMessageType.Bytes);
        publisher.setErrorHandlerLoggingLevel(LoggingLevel.ERROR);
        publisher.setDeliveryPersistent(true);
        publisher.setDeliveryMode(2);
        return publisher;
    }

    @Bean("subscribe")
    public JmsComponent subscribe() {
        JmsComponent subscriber = JmsComponent.jmsComponent(inboundConnectionFactory());
        subscriber.setTransacted(true);
        subscriber.setLazyCreateTransactionManager(false);
        subscriber.setCacheLevelName("CACHE_CONSUMER");
        subscriber.setDisableReplyTo(true);
        subscriber.setTestConnectionOnStartup(true);
        subscriber.setJmsMessageType(JmsMessageType.Bytes);
        subscriber.setErrorHandlerLoggingLevel(LoggingLevel.ERROR);
        subscriber.setSubscriptionDurable(true);
        subscriber.setAcceptMessagesWhileStopping(true);
        return subscriber;
    }

    private static final String BUFFERED_MESSAGE_STORE_CHANNEL_PREFIX = "bufferedOutputMessageChannel.";
    private static final String MESSAGE_STORE_PRODUCER_GROUP_PREFIX = "bufferedOutputMessageChannel.";

    @Bean
    public IntegrationMessagingRouteTemplateParameterSource fakeIntegrationMessagingRouteTemplateParameterSource() {
        return new IntegrationMessagingRouteTemplateParameterSource();
    }

    @Bean("jdbcChannelMessageStore")
    public JdbcChannelMessageStore jdbcChannelMessageStore(DefaultListableBeanFactory beanFactory) {
        JdbcChannelMessageStore store = new JdbcChannelMessageStore();
        store.setDataSource(beanFactory.getBean(DataSource.class));
        store.setPriorityEnabled(true);
        store.setChannelMessageStoreQueryProvider(
                determineDevDatabase(beanFactory)
                        ? new H2ChannelMessageStoreQueryProvider()
                        : new MySqlChannelMessageStoreQueryProvider());
        store.setTablePrefix("TEST_");
        return store;
    }

    @Bean
    public MessageChannel outboundMessageChannel() {
        return MessageChannels.direct().get();
    }

    @Bean("io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.contextConfiguration")
    @Order
    public CamelContextConfiguration contextConfiguration(IntegrationFlowContext context,
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
                    IntegrationMessagingRouteConfiguration.AbstractIntegrationMessagingDefinition definition = source.parameter(routeId);
                    if (definition instanceof IntegrationMessagingRouteConfiguration.IntegrationMessagingProducerDefinition) {
                        registerMessageStoreIntegrationFlow(
                                context,
                                beanFactory,
                                jdbcChannelMessageStore,
                                outboundChainedTxManager,
                                ((IntegrationMessagingRouteConfiguration.IntegrationMessagingProducerDefinition) definition).getMessagePublishEndpoint());
                    }
                }

            }
        };
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(2);
        taskExecutor.setQueueCapacity(2);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return taskExecutor;
    }

    @SuppressWarnings("unchecked")
    private void registerMessageStoreIntegrationFlow(IntegrationFlowContext context,
                                                     DefaultListableBeanFactory beanFactory,
                                                     JdbcChannelMessageStore jdbcChannelMessageStore,
                                                     ChainedTransactionManager outboundChainedTxManager,
                                                     String topicName) {
        MessageChannel channel = MessageChannels
                .priority(BUFFERED_MESSAGE_STORE_CHANNEL_PREFIX + topicName)
                .messageStore(jdbcChannelMessageStore, MESSAGE_STORE_PRODUCER_GROUP_PREFIX + topicName)
                .get();
        BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(
                        (Class<MessageChannel>) channel.getClass(),
                        () -> channel)
                .getRawBeanDefinition();
        beanFactory.registerBeanDefinition(BUFFERED_MESSAGE_STORE_CHANNEL_PREFIX + topicName ,beanDefinition);
        IntegrationFlow flow = IntegrationFlows
                .from(BUFFERED_MESSAGE_STORE_CHANNEL_PREFIX + topicName)
                .bridge(e -> e.poller(
                        Pollers.fixedDelay(5000)
                                .maxMessagesPerPoll(-1)
                                .transactional(new TransactionInterceptorBuilder()
                                        .transactionManager(outboundChainedTxManager)
                                        .timeout(determineDevDatabase(beanFactory) ? 0 : -1)
                                        .readOnly(false)
                                        .propagation(Propagation.REQUIRES_NEW)
                                        .build()).taskExecutor(taskExecutor())))
                .channel(outboundMessageChannel())
                .get();
        context.registration(flow).register();
    }

    private boolean determineDevDatabase(DefaultListableBeanFactory beanFactory) {
        DataSourceProperties dataSourceProperties = beanFactory.getBean(DataSourceProperties.class);
        return DatabaseDriver.fromJdbcUrl(dataSourceProperties.determineUrl()).equals(DatabaseDriver.H2);
    }

    private ActiveMQConnectionFactory createConnectionFactory() {
        try {
            TransportConfiguration transportConfiguration = new TransportConfiguration(
                    InVMConnectorFactory.class.getName(),
                    artemisProperties.getEmbedded().generateTransportParameters());
            ServerLocator serviceLocator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
            return ActiveMQConnectionFactory.class.getConstructor(ServerLocator.class).newInstance(serviceLocator);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Unable to create InVM "
                    + "Artemis connection, ensure that artemis-jms-server.jar is in the classpath", ex);
        }
    }
}
