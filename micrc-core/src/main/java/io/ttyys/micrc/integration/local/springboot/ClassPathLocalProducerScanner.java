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
            // 解析获取注解上的值,要获取注解上的endpoint 然后放到上面那个LocalConsumerRoutersInfo里 直接由Spring生成
            AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) holder.getBeanDefinition();
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                    annotatedBeanDefinition.getMetadata().getAnnotationAttributes(LocalTransferProducer.class.getName()));
            Object endpoint = attributes.get("endpoint");
            Object adapterClassName = attributes.get("adapterClassName");
            Map<String, Object> params = new HashMap<>();
            params.put("endpoint", endpoint);
            params.put("adapterClassName", adapterClassName);
            routersInfo.add(params);
            // 这里要反射获取类下面的所有方法的方法名称和入参值
            GenericBeanDefinition genericBeanDefinition = (GenericBeanDefinition) holder.getBeanDefinition();
            // 加载类并提取接口信息
            genericBeanDefinition.resolveBeanClass(Thread.currentThread().getContextClassLoader());
            String beanClassName = genericBeanDefinition.getBeanClass().getName();
            params.put("beanClassName", beanClassName);
            // 创建动态代理
            genericBeanDefinition.getConstructorArgumentValues()
                    .addGenericArgumentValue(Objects.requireNonNull(genericBeanDefinition.getBeanClassName()));
            genericBeanDefinition.getConstructorArgumentValues()
                    .addGenericArgumentValue(endpoint);
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