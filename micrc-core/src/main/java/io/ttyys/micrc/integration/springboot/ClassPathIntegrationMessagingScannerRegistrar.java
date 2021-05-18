package io.ttyys.micrc.integration.springboot;

import io.ttyys.micrc.annotations.technology.integration.MessageConsumer;
import io.ttyys.micrc.annotations.technology.integration.MessageProducer;
import io.ttyys.micrc.integration.EnableMessagingIntegration;
import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration;
import io.ttyys.micrc.integration.route.IntegrationMessagingRouteTemplateParameterSource;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.IntegrationMessagingProducerDefinition;
import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.IntegrationMessagingConsumerDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClassPathIntegrationMessagingScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @SuppressWarnings("unchecked")
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry,
                                        BeanNameGenerator importBeanNameGenerator) {
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
        // registering
        BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(
                        (Class<IntegrationMessagingRouteTemplateParameterSource>) source.getClass(),
                        () -> source)
                .getRawBeanDefinition();
        // remove route template source bean registered from IntegrationMessagingAutoConfiguration
        registry.removeBeanDefinition("fakeIntegrationMessagingRouteTemplateParameterSource");
        registry.registerBeanDefinition(importBeanNameGenerator.generateBeanName(beanDefinition, registry),
                beanDefinition);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}

class MessageProducerScanner extends ClassPathBeanDefinitionScanner {

    private static final AtomicInteger INDEX = new AtomicInteger();
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

    @SneakyThrows
    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        this.addIncludeFilter(new AnnotationTypeFilter(MessageProducer.class));
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        for (BeanDefinitionHolder holder : holders) {
            GenericBeanDefinition beanDefinition = (GenericBeanDefinition) holder.getBeanDefinition();
            beanDefinition.resolveBeanClass(Thread.currentThread().getContextClassLoader());
            MessageProducer messageProducer = beanDefinition.getBeanClass().getAnnotation(MessageProducer.class);
            sourceDefinition.addParameter(
                    routeId(messageProducer),
                    IntegrationMessagingProducerDefinition.builder()
                            .templateId(IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL)
                            .messagePublishEndpoint(messageProducer.messagePublishEndpoint())
                            .build());
        }
        holders.clear();
        return holders;
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry routersInfo) {
        // nothing to do. leave it out.
    }

    private String routeId(MessageProducer messageConsumer) {
        String routeId = messageConsumer.id();
        if (!StringUtils.hasText(routeId)) {
            routeId = String.valueOf(INDEX.getAndIncrement());
        }
        return IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_PUBLISH_INTERNAL + "-" + routeId;
    }
}

class MessageConsumerScanner extends ClassPathBeanDefinitionScanner {

    private final AtomicInteger INDEX = new AtomicInteger();
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

    @SneakyThrows
    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        this.addIncludeFilter(new AnnotationTypeFilter(MessageConsumer.class));
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        for (BeanDefinitionHolder holder : holders) {
            GenericBeanDefinition beanDefinition = (GenericBeanDefinition) holder.getBeanDefinition();
            beanDefinition.resolveBeanClass(Thread.currentThread().getContextClassLoader());
            MessageConsumer messageConsumer = beanDefinition.getBeanClass().getAnnotation(MessageConsumer.class);
            sourceDefinition.addParameter(
                    routeId(messageConsumer),
                    IntegrationMessagingConsumerDefinition.builder()
                            .templateId(IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION)
                            .topicName(messageConsumer.topicName())
                            .subscriptionName(messageConsumer.subscriptionName())
                            .adapterName(messageConsumer.adapterName())
                            .build());
        }
        holders.clear();
        return holders;
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry routersInfo) {
        // nothing to do. leave it out.
    }

    private String routeId(MessageConsumer messageConsumer) {
        String routeId = messageConsumer.id();
        if (!StringUtils.hasText(routeId)) {
            routeId = String.valueOf(INDEX.getAndIncrement());
        }
        return IntegrationMessagingRouteConfiguration.ROUTE_TMPL_MESSAGE_SUBSCRIPTION + "-" + routeId;
    }
}
