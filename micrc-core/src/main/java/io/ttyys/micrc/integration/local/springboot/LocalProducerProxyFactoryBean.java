package io.ttyys.micrc.integration.local.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

public class LocalProducerProxyFactoryBean<T> implements FactoryBean<T>, CamelContextAware {
    private final Class<T> interfaceType;

    private final String endpoint;

    private CamelContext camelContext;

    public LocalProducerProxyFactoryBean(Class<T> interfaceType, String endpoint) {
        this.interfaceType = interfaceType;
        this.endpoint = endpoint;
    }

    @SuppressWarnings({"unchecked", "RedundantThrows"})
    @Override
    public T getObject() throws Exception {
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class[]{interfaceType},
                new LocalProducerProxy<>(interfaceType, this.camelContext, endpoint));
    }

    @Override
    public Class<?> getObjectType() {
        return this.interfaceType;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }
}
