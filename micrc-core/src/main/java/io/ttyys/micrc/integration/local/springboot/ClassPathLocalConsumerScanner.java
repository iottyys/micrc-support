package io.ttyys.micrc.integration.local.springboot;

import com.google.common.collect.Maps;
import io.ttyys.micrc.annotations.technology.LocalTransferConsumer;
import io.ttyys.micrc.integration.local.camel.LocalConsumerRoutesInfo;
import lombok.SneakyThrows;
import org.apache.avro.data.Json;
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
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 本地同进程消息接收端注解扫描器
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/4/25 7:34 下午
 */
public class ClassPathLocalConsumerScanner extends ClassPathBeanDefinitionScanner {

    public ClassPathLocalConsumerScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    @SneakyThrows(ClassNotFoundException.class)
    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        List<Map<String, Object>> routersInfo = new ArrayList<>();
        this.addIncludeFilter(new AnnotationTypeFilter(LocalTransferConsumer.class));
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        for (BeanDefinitionHolder holder : holders) {
            GenericBeanDefinition beanDefinition = (GenericBeanDefinition) holder.getBeanDefinition();
            beanDefinition.resolveBeanClass(Thread.currentThread().getContextClassLoader());

            Class<?> beanClass = beanDefinition.getBeanClass();
            LocalTransferConsumer localTransferConsumer = beanClass.getAnnotation(LocalTransferConsumer.class);
            Map<String, Object> params = new HashMap<>();
            params.put("endpoint", localTransferConsumer.endpoint());
            params.put("adapterClassName", localTransferConsumer.adapterClassName());
            routersInfo.add(params);
            setMethodSignature(beanDefinition, params);
        }
        this.registerRoutersInfo(super.getRegistry(), routersInfo);
        // 清除该接口的拦截实现
        holders.clear();
        return holders;
    }

    private void setMethodSignature(GenericBeanDefinition beanDefinition, Map<String, Object> params) throws ClassNotFoundException {
        Map<String, Object> methodsInfo = new HashMap<>();
        ReflectionUtils.doWithLocalMethods(beanDefinition.getBeanClass(), method -> {
            // 这里要反射获取类下面的所有方法的方法名称和入参值
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            assert parameterTypes.length <= 1;
            Map<String, Object> currentMethod = Maps.newHashMap();
            if (1 == parameterTypes.length) {
                currentMethod.put("parameterType", parameterTypes[0].getName());
            }
            currentMethod.put("returnType", method.getReturnType().getName());
            methodsInfo.put(methodName, Json.toString(currentMethod));
        });
        params.put("methodSignature", Json.toString(methodsInfo));
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.isInterface() && metadata.isIndependent();
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry routersInfo) {
    }

    private void registerRoutersInfo(BeanDefinitionRegistry registry, List<Map<String, Object>> routersInfo) {
        GenericBeanDefinition routeInfoDefinition = new GenericBeanDefinition();
        routeInfoDefinition.getConstructorArgumentValues().addGenericArgumentValue(routersInfo);
        routeInfoDefinition.setBeanClass(LocalConsumerRoutesInfo.class);
        routeInfoDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        routeInfoDefinition.setLazyInit(false);
        routeInfoDefinition.setPrimary(true);
        String beanName = AnnotationBeanNameGenerator.INSTANCE.generateBeanName(routeInfoDefinition, registry);
        BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(routeInfoDefinition, beanName);
        super.registerBeanDefinition(definitionHolder, registry);
    }
}
