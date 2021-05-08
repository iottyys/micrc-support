package io.ttyys.micrc.integration.springboot;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.EndpointInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsMessageType;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
@SpringBootApplication
public class SimpleJmsTest {

    @EndpointInject("jms:topic:pub1")
    private ProducerTemplate testConsumerProducer;

    @EndpointInject("direct:send.message.pub1")
    private ProducerTemplate testProducerProducer;

    @Autowired
    private EmbeddedActiveMQ server;

    @Test
    public void testConfiguration() {
        assertThat(server).isNotNull();
        assertThat(server.getActiveMQServer().getConfiguration().getJournalDirectory())
                .isEqualTo("");
    }

    @Test
    public void testConsumer() throws InterruptedException {
        testConsumerProducer.sendBody("");
        Thread.sleep(3000);
    }

    @Test
    public void testProducer() {
        testProducerProducer.requestBody("3");
    }

    @Configuration
    @EnableConfigurationProperties({ ArtemisProperties.class, JmsProperties.class })
    @Import({JpaTransactionManager.class})
    static class IntegrationMessagingTransactionConfiguration {

        private final ArtemisProperties artemisProperties;

        @Autowired
        public IntegrationMessagingTransactionConfiguration(ArtemisProperties artemisProperties) {
            this.artemisProperties = artemisProperties;
        }

        @Bean
        public CachingConnectionFactory inboundConnectionFactory() {
            try {
                TransportConfiguration transportConfiguration = new TransportConfiguration(
                        InVMConnectorFactory.class.getName(),
                        artemisProperties.getEmbedded().generateTransportParameters());
                ServerLocator serviceLocator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
                return new CachingConnectionFactory(
                        ActiveMQConnectionFactory.class.getConstructor(ServerLocator.class).newInstance(serviceLocator));
            }
            catch (Exception ex) {
                throw new IllegalStateException("Unable to create InVM "
                        + "Artemis connection, ensure that artemis-jms-server.jar is in the classpath", ex);
            }
        }

        @Bean
        public CachingConnectionFactory outboundConnectionFactory() {
            try {
                TransportConfiguration transportConfiguration = new TransportConfiguration(
                        InVMConnectorFactory.class.getName(),
                        artemisProperties.getEmbedded().generateTransportParameters());
                ServerLocator serviceLocator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
                return new CachingConnectionFactory(
                        ActiveMQConnectionFactory.class.getConstructor(ServerLocator.class).newInstance(serviceLocator));
            }
            catch (Exception ex) {
                throw new IllegalStateException("Unable to create InVM "
                        + "Artemis connection, ensure that artemis-jms-server.jar is in the classpath", ex);
            }
        }

        @Bean
        public JmsTransactionManager inboundTransactionManager() {
            JmsTransactionManager transactionManager = new JmsTransactionManager();
            transactionManager.setConnectionFactory(inboundConnectionFactory());
            return transactionManager;
        }

        @Bean
        public JmsTransactionManager outboundTransactionManager() {
            JmsTransactionManager transactionManager = new JmsTransactionManager();
            transactionManager.setConnectionFactory(outboundConnectionFactory());
            return transactionManager;
        }

        @Bean
        public ChainedTransactionManager inboundChainedTxManager(JpaTransactionManager jpaTransactionManager) {
            return new ChainedTransactionManager(inboundTransactionManager(), jpaTransactionManager);
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
            subscriber.setTransactionManager(inboundTransactionManager());
            subscriber.setDisableReplyTo(true);
            subscriber.setTestConnectionOnStartup(true);
            subscriber.setJmsMessageType(JmsMessageType.Bytes);
            subscriber.setErrorHandlerLoggingLevel(LoggingLevel.ERROR);
            subscriber.setSubscriptionDurable(true);
            subscriber.setAcceptMessagesWhileStopping(true);
            return subscriber;
        }
    }

    @Configuration
    static class ArtemisEmbeddedServerBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof EmbeddedActiveMQ) {
                EmbeddedActiveMQ server = (EmbeddedActiveMQ) bean;
//                server.getActiveMQServer().getConfiguration().setPersistenceEnabled(true);
                server.getActiveMQServer().getConfiguration().setJournalDirectory("");
                server.getActiveMQServer().getConfiguration().setBindingsDirectory("");
                server.getActiveMQServer().getConfiguration().setLargeMessagesDirectory("");
                server.getActiveMQServer().getConfiguration().setPagingDirectory("");
            }
            return bean;
        }
    }
}
