package io.ttyys.micrc.integration.local.springboot;

import com.google.common.collect.Maps;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class LocalProducerProxy<T> implements InvocationHandler {

    private final Class<T> interfaceType;

    private final CamelContext camelContext;

    private final String endpoint;

    public LocalProducerProxy(Class<T> interfaceType, CamelContext camelContext, String endpoint) {
        this.interfaceType = interfaceType;
        this.camelContext = camelContext;
        this.endpoint = endpoint;

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args.length > 1) {
            throw new IllegalAccessError("method param is error");
        }
        // FIXME 这里要跳过代理tostring这些方法 // com.google.common.reflect.AbstractInvocationHandler用来处理hashcode, equals和toString
        // https://stackoverflow.com/questions/39507736/dynamic-proxy-bean-with-autowiring-capability
        if (method.getDeclaringClass().equals(interfaceType)) {
            Method[] declaredMethods = interfaceType.getDeclaredMethods();
            if (Arrays.stream(declaredMethods).anyMatch(declaredMethod -> declaredMethod.getName().equals(method.getName()))) {
                // 方法存在于该类中,需要转发路由
                // 这里发送到发送端真实路由上,路由的endpoint是beanClassName(全路径不重名)
                ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
                Map<String, Object> headers = Maps.newHashMap();
                headers.put("methodName", method.getName());
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1) {
                    headers.put("methodParameterType", parameterTypes[0].getName());
                }
                headers.put("methodReturnType", method.getReturnType().getName());

                Object result =  producerTemplate.requestBodyAndHeaders("direct:" + interfaceType.getName(), args[0], headers);
                return result;
            }
            // 因为是本地消息,发送无返回
            return null;
        } else {
            throw new IllegalAccessError("unexpect method invoke.");
        }
    }
}
