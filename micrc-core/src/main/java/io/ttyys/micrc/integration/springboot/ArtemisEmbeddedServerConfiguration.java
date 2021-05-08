package io.ttyys.micrc.integration.springboot;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.server.config.JMSConfiguration;
import org.apache.activemq.artemis.jms.server.config.JMSQueueConfiguration;
import org.apache.activemq.artemis.jms.server.config.TopicConfiguration;
import org.apache.activemq.artemis.jms.server.config.impl.JMSConfigurationImpl;
import org.apache.activemq.artemis.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.apache.activemq.artemis.jms.server.config.impl.TopicConfigurationImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EmbeddedActiveMQ.class)
class ArtemisEmbeddedServerConfiguration {

    private final ArtemisProperties properties;

    ArtemisEmbeddedServerConfiguration(ArtemisProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    org.apache.activemq.artemis.core.config.Configuration artemisConfiguration() {
        return new ArtemisEmbeddedConfigurationFactory(this.properties).createConfiguration();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    EmbeddedActiveMQ embeddedActiveMq(org.apache.activemq.artemis.core.config.Configuration configuration,
                                      JMSConfiguration jmsConfiguration, ObjectProvider<ArtemisConfigurationCustomizer> configurationCustomizers) {
        for (JMSQueueConfiguration queueConfiguration : jmsConfiguration.getQueueConfigurations()) {
            String queueName = queueConfiguration.getName();
            configuration.addAddressConfiguration(
                    new CoreAddressConfiguration().setName(queueName).addRoutingType(RoutingType.ANYCAST)
                            .addQueueConfiguration(new QueueConfiguration(queueName).setAddress(queueName)
                                    .setFilterString(queueConfiguration.getSelector())
                                    .setDurable(queueConfiguration.isDurable()).setRoutingType(RoutingType.ANYCAST)));
        }
        for (TopicConfiguration topicConfiguration : jmsConfiguration.getTopicConfigurations()) {
            configuration.addAddressConfiguration(new CoreAddressConfiguration().setName(topicConfiguration.getName())
                    .addRoutingType(RoutingType.MULTICAST));
        }
        configurationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configuration));
        EmbeddedActiveMQ embeddedActiveMq = new EmbeddedActiveMQ();
        standardServerConfiguration(configuration);
        embeddedActiveMq.setConfiguration(configuration);
        return embeddedActiveMq;
    }

    @Bean
    @ConditionalOnMissingBean
    JMSConfiguration artemisJmsConfiguration(ObjectProvider<JMSQueueConfiguration> queuesConfiguration,
                                             ObjectProvider<TopicConfiguration> topicsConfiguration) {
        JMSConfiguration configuration = new JMSConfigurationImpl();
        addAll(configuration.getQueueConfigurations(), queuesConfiguration);
        addAll(configuration.getTopicConfigurations(), topicsConfiguration);
        addQueues(configuration, this.properties.getEmbedded().getQueues());
        addTopics(configuration, this.properties.getEmbedded().getTopics());
        return configuration;
    }

    private void standardServerConfiguration(org.apache.activemq.artemis.core.config.Configuration configuration) {
        // todo 可靠投递 -- 不考虑集群下的共享存储，使用列队复制。考虑k8s部署场景
//        configuration.setPersistenceEnabled(true);
        configuration.setJournalDirectory("");
        configuration.setBindingsDirectory("");
        configuration.setPagingDirectory("");
        configuration.setLargeMessagesDirectory("");
        configuration.setNodeManagerLockDirectory("");
        // todo 发送消重
        // todo 组建集群 -- 针对当前服务负载均衡和多服务情况下的队列复制
        // todo HA -- ActiveMQClient.createServerLocatorWithoutHA不影响，服务本身HA，不影响发送接收消息
    }

    private <T> void addAll(List<T> list, ObjectProvider<T> items) {
        if (items != null) {
            list.addAll(items.orderedStream().collect(Collectors.toList()));
        }
    }

    private void addQueues(JMSConfiguration configuration, String[] queues) {
        boolean persistent = this.properties.getEmbedded().isPersistent();
        for (String queue : queues) {
            JMSQueueConfigurationImpl jmsQueueConfiguration = new JMSQueueConfigurationImpl();
            jmsQueueConfiguration.setName(queue);
            jmsQueueConfiguration.setDurable(persistent);
            jmsQueueConfiguration.setBindings("/queue/" + queue);
            configuration.getQueueConfigurations().add(jmsQueueConfiguration);
        }
    }

    private void addTopics(JMSConfiguration configuration, String[] topics) {
        for (String topic : topics) {
            TopicConfigurationImpl topicConfiguration = new TopicConfigurationImpl();
            topicConfiguration.setName(topic);
            topicConfiguration.setBindings("/topic/" + topic);
            configuration.getTopicConfigurations().add(topicConfiguration);
        }
    }

    static class ArtemisEmbeddedConfigurationFactory {

        private static final Log logger = LogFactory.getLog(ArtemisEmbeddedConfigurationFactory.class);

        private final ArtemisProperties.Embedded properties;

        ArtemisEmbeddedConfigurationFactory(ArtemisProperties properties) {
            this.properties = properties.getEmbedded();
        }

        org.apache.activemq.artemis.core.config.Configuration createConfiguration() {
            ConfigurationImpl configuration = new ConfigurationImpl();
            configuration.setSecurityEnabled(false);
            configuration.setPersistenceEnabled(this.properties.isPersistent());
            String dataDir = getDataDir();
            configuration.setJournalDirectory(dataDir + "/journal");
            if (this.properties.isPersistent()) {
                configuration.setJournalType(JournalType.NIO);
                configuration.setLargeMessagesDirectory(dataDir + "/largemessages");
                configuration.setBindingsDirectory(dataDir + "/bindings");
                configuration.setPagingDirectory(dataDir + "/paging");
            }
            TransportConfiguration transportConfiguration = new TransportConfiguration(InVMAcceptorFactory.class.getName(),
                    this.properties.generateTransportParameters());
            configuration.getAcceptorConfigurations().add(transportConfiguration);
            if (this.properties.isDefaultClusterPassword() && logger.isDebugEnabled()) {
                logger.debug("Using default Artemis cluster password: " + this.properties.getClusterPassword());
            }
            configuration.setClusterPassword(this.properties.getClusterPassword());
            configuration.addAddressConfiguration(createAddressConfiguration("DLQ"));
            configuration.addAddressConfiguration(createAddressConfiguration("ExpiryQueue"));
            configuration.addAddressesSetting("#",
                    new AddressSettings().setDeadLetterAddress(SimpleString.toSimpleString("DLQ"))
                            .setExpiryAddress(SimpleString.toSimpleString("ExpiryQueue")));
            return configuration;
        }

        private CoreAddressConfiguration createAddressConfiguration(String name) {
            return new CoreAddressConfiguration().setName(name).addRoutingType(RoutingType.ANYCAST).addQueueConfiguration(
                    new QueueConfiguration(name).setRoutingType(RoutingType.ANYCAST).setAddress(name));
        }

        private String getDataDir() {
            if (this.properties.getDataDirectory() != null) {
                return this.properties.getDataDirectory();
            }
            String tempDirectory = System.getProperty("java.io.tmpdir");
            return new File(tempDirectory, "artemis-data").getAbsolutePath();
        }

    }


}
