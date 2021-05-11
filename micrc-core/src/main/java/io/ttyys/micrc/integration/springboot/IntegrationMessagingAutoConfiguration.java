package io.ttyys.micrc.integration.springboot;

import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration;
import io.ttyys.micrc.persistence.springboot.PersistenceAutoConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsMessageType;
import org.apache.camel.component.jms.springboot.JmsComponentAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.jms.ConnectionFactory;

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