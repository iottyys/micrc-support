package io.ttyys.micrc.integration.local.camel;

import org.apache.avro.data.Json;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 本地消息消费端路由模版
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/4/28 6:10 下午
 */
public class LocalConsumerRouteTemplate extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        routeTemplate("localConsumerRouteTemplate")
                .templateParameter("endpoint")
                .templateParameter("adapterClassName")
                .templateParameter("methodSignature")
                .from("direct:{{endpoint}}")
                .routeId("consumer-{{adapterClassName}}")
                .setHeader("methodSignature", simple("{{methodSignature}}"))
                .process(exchange -> {
                    Message message = exchange.getIn();
                    //noinspection unchecked
                    Map<String, String> methodMap = (Map<String, String>) Json.parseJson(message.getHeader("methodSignature", String.class));
                    //noinspection unchecked
                    Map<String, String> methodInfo = (Map<String, String>) Json.parseJson(methodMap.get(message.getHeader("methodName", String.class)));
                    String parameterType = methodInfo.get("parameterType");
                    String returnType = methodInfo.get("returnType");
                    if (StringUtils.isNotBlank(parameterType)) {
                        message.setHeader("CamelJacksonUnmarshalType", parameterType);
                    }
                    if (StringUtils.isNotBlank(returnType)) { // TODO  无返回值的判断--有待验证
                        message.setHeader("currentReturnType", returnType);
                    }
                })

                .choice()
                .when(header("CamelJacksonUnmarshalType").isNotNull())
                // avro转换必须实现接口GenericContainer中的getSchema方法，所以后期通过avro schema自动生成的类可以这么转换， 暂时只能用json进行转换
                .to("dataformat:json-jackson:unmarshal?allow-unmarshall-type=true")
                .end()

                .setBody(simple("${body}"))
                .toD("bean:{{adapterClassName}}?method=${headers.methodName}")

                .choice()
                .when(header("currentReturnType").isNotNull())
                .marshal().json()
                .end()
        ;
    }
}
