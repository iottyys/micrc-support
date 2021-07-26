package io.ttyys.micrc.integration.local.springboot;

import io.ttyys.micrc.annotations.technology.LocalTransferProducer;
import io.ttyys.micrc.integration.local.camel.LocalProducerRoutesInfo;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.*;

import static org.jgroups.blocks.RpcDispatcher.getName;

/**
 * 本息同进程消息发送端注解扫描器
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/4/25 7:34 下午
 */
public class ClassPathLocalProducerScanner extends ClassPathBeanDefinitionScanner {

    public ClassPathLocalProducerScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    @SneakyThrows(ClassNotFoundException.class)
    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        List<Map<String, Object>> routersInfo = new ArrayList<>();
        this.addIncludeFilter(new AnnotationTypeFilter(LocalTransferProducer.class));
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        for (BeanDefinitionHolder holder : holders) {
            GenericBeanDefinition genericBeanDefinition = (GenericBeanDefinition) holder.getBeanDefinition();
            // 加载类并提取接口信息
            genericBeanDefinition.resolveBeanClass(Thread.currentThread().getContextClassLoader());
            Class<?> beanClass = genericBeanDefinition.getBeanClass();
            LocalTransferProducer localTransferProducer = beanClass.getAnnotation(LocalTransferProducer.class);
            Map<String, Object> params = new HashMap<>();
            params.put("endpoint", localTransferProducer.endpoint() + localTransferProducer.value());
            params.put("beanClassName", beanClass.getName());
            routersInfo.add(params);
            // 创建动态代理
            genericBeanDefinition.getConstructorArgumentValues()
                    .addGenericArgumentValue(Objects.requireNonNull(genericBeanDefinition.getBeanClassName()));
            genericBeanDefinition.getConstructorArgumentValues()
                    .addGenericArgumentValue(localTransferProducer.endpoint());
            genericBeanDefinition.setBeanClass(LocalProducerProxyFactoryBean.class);
            genericBeanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            genericBeanDefinition.setPrimary(false);
            genericBeanDefinition.setScope("singleton");
        }
        this.registerRoutersInfo(super.getRegistry(), routersInfo);
        return holders;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.isInterface() && metadata.isIndependent();
    }

    private void registerRoutersInfo(BeanDefinitionRegistry registry, List<Map<String, Object>> routersInfo) {
        GenericBeanDefinition routeInfoDefinition = new GenericBeanDefinition();
        routeInfoDefinition.getConstructorArgumentValues().addGenericArgumentValue(routersInfo);
        routeInfoDefinition.setBeanClass(LocalProducerRoutesInfo.class);
        routeInfoDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        routeInfoDefinition.setLazyInit(false);
        routeInfoDefinition.setPrimary(true);
        String beanName = AnnotationBeanNameGenerator.INSTANCE.generateBeanName(routeInfoDefinition, registry);
        BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(routeInfoDefinition, beanName);
        super.registerBeanDefinition(definitionHolder, registry);
    }
}