package io.ttyys.micrc.integration.springboot;

import io.ttyys.micrc.integration.EnableMessagingIntegration;
import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration;
import io.ttyys.micrc.integration.route.IntegrationMessagingRouteTemplateParameterSource;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.IntegrationMessagingProducerDefinition;
import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.IntegrationMessagingConsumerDefinition;

public class ClassPathIntegrationMessagingScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes
                .fromMap(importingClassMetadata.getAnnotationAttributes(EnableMessagingIntegration.class.getName()));
        assert attributes != null;
        String[] basePackages = attributes.getStringArray("basePackages");
        if (basePackages.length == 0 && importingClassMetadata instanceof StandardAnnotationMetadata) {
            basePackages = new String[]{((StandardAnnotationMetadata) importingClassMetadata)
                    .getIntrospectedClass().getPackage().getName()};
        }
        if (basePackages.length == 0) {
            return;
        }
        IntegrationMessagingRouteTemplateParameterSource source = new IntegrationMessagingRouteTemplateParameterSource();
        // producer
        MessageProducerScanner producerScanner = new MessageProducerScanner(registry, source);
        producerScanner.setResourceLoader(resourceLoader);
        producerScanner.doScan(basePackages);
        // consumer
        MessageConsumerScanner consumerScanner = new MessageConsumerScanner(registry, source);
        consumerScanner.setResourceLoader(resourceLoader);
        consumerScanner.doScan(basePackages);
        //noinspection unchecked
        BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(
                        (Class<IntegrationMessagingRouteTemplateParameterSource>) source.getClass(),
                        () -> source)
                .getRawBeanDefinition();
        registry.registerBeanDefinition("integrationMessagingRouteTemplateParameterSource",
                beanDefinition);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}

class MessageProducerScanner extends ClassPathBeanDefinitionScanner {

    private final IntegrationMessagingRouteTemplateParameterSource sourceDefinition;

    public MessageProducerScanner(BeanDefinitionRegistry registry,
                                  IntegrationMessagingRouteTemplateParameterSource source) {
        super(registry, false);
        this.sourceDefinition = source;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.isInterface() && metadata.isIndependent();
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        this.addIncludeFilter(new AnnotationTypeFilter(MessageProducer.class));
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        for (BeanDefinitionHolder holder : holders) {
            AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) holder.getBeanDefinition();
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                    annotatedBeanDefinition.getMetadata().getAnnotationAttributes(MessageProducer.class.getName()));
            // todo obtain properties
            // attributes.getXXX();
            // method information of target interface

        }
        // fake data. move the logic to inner of loop
//        IntegrationMessagingProducerDefinition definition = new IntegrationMessagingProducerDefinition();

        sourceDefinition.addParameter(
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL + "-0",
                IntegrationMessagingProducerDefinition.builder()
                        .templateId(IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL)
                        .messagePublishEndpoint("demo.test.topic").build());

        holders.clear();
        return holders;
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry routersInfo) {
        // nothing to do. leave it out.
    }
}

class MessageConsumerScanner extends ClassPathBeanDefinitionScanner {

    private final IntegrationMessagingRouteTemplateParameterSource sourceDefinition;

    public MessageConsumerScanner(BeanDefinitionRegistry registry,
                                  IntegrationMessagingRouteTemplateParameterSource source) {
        super(registry, false);
        this.sourceDefinition = source;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.isInterface() && metadata.isIndependent();
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        this.addIncludeFilter(new AnnotationTypeFilter(MessageConsumer.class));
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        for (BeanDefinitionHolder holder : holders) {
            AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) holder.getBeanDefinition();
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                    annotatedBeanDefinition.getMetadata().getAnnotationAttributes(MessageConsumer.class.getName()));
            // todo obtain properties
            // attributes.getXXX();

        }

        // fake data. move the logic to inner of loop
        sourceDefinition.addParameter(
                IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-0",
                IntegrationMessagingConsumerDefinition.builder()
                        .templateId(IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION)
                        .topicName("test.without.db_tx.topic")
                        .subscriptionName("test.sub")
                        .adapterName("demoMessageAdapter").build());

        holders.clear();
        return holders;
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry routersInfo) {
        // nothing to do. leave it out.
    }
}

// todo move to micrc-annotation then append necessary properties
@interface MessageProducer {}

@interface MessageConsumer {}
