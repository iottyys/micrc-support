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
                        message.setHeader("currentParameterType", parameterType);
                    }
                    if (StringUtils.isNotBlank(returnType)) { // TODO  无返回值的判断--有待验证
                        message.setHeader("currentReturnType", returnType);
                    }
                    log.info(message.getHeaders().toString());
                    log.info(message.getBody().toString());
                })
                .log("${headers}").log("${body}")
                .choice()
                .when(header("currentParameterType").isNotNull())
                .unmarshal().json()
//                .to("dataformat:json:unmarshal?contextPath=${headers.currentParameterType}")
//                .to("dataformat:avro:unmarshal?contextPath=${headers.currentParameterType}")
//                .unmarshal().avro(simple("${headers.currentParameterType}", String.class))
                .end()
                // .bean("#{{adapterClassName}}", "-D${headers.methodName}(${body})")
                .setBody(simple("${body}"))
                .toD("bean:{{adapterClassName}}?method=${headers.methodName}")
                .choice()
                .when(header("currentReturnType").isNotNull())
                .marshal().json()
//                .toD("dataformat:avro:marshal?contextPath=${headers.currentReturnType}")
//                .marshal().avro(header("currentReturnType"))
                .end()
                .process(exchange -> {
                    Message message = exchange.getIn();
                    log.info(message.getHeaders().toString());
                    log.info(message.getBody().toString());
                })
        ;
    }
}
