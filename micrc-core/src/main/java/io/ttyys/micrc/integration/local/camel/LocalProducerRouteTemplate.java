package io.ttyys.micrc.integration.local.camel;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

/**
 * 本地消息生产端路由模版
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/4/28 6:10 下午
 */
@Slf4j
public class LocalProducerRouteTemplate extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        routeTemplate("localProducerRouteTemplate")
                .templateParameter("endpoint")
                .templateParameter("beanClassName")
                .templateParameter("adapterClassName")
                .from("direct:{{beanClassName}}")
                .routeId("producer-{{beanClassName}}")
                .process(exchange -> {
                    Message message = exchange.getIn();
                    log.info(message.getHeaders().toString());
                    log.info(message.getBody().toString());
                })
                .log("${headers}").log("${body}")
                // FIXME 这里需要修改为avro转换
                // https://stackoverflow.com/questions/40756027/apache-camel-json-marshalling-to-pojo-java-bean
                // https://camel.apache.org/components/3.9.x/dataformats/json-johnzon-dataformat.html
                .choice()
                .when(header("methodParameterType").isNotNull())
                .marshal().json()
//                .toD("dataformat:avro:marshal?contextPath=${headers.methodParameterType}")
//                .marshal().avro(simple("${headers.methodParameterType}", String.class).toString())
                .end()
                .setBody(simple("${body}"))
                .to("direct:{{endpoint}}")
                .process(exchange -> {
                    Message message = exchange.getIn();
                    log.info(message.getHeaders().toString());
                    log.info(message.getBody().toString());
                })
//                .choice()
//                .when(header("methodReturnType").isNotNull())
                //.unmarshal().json(JsonLibrary.Jackson, simple(""))
//                .to("dataformat:json:unmarshal?contextPath=${headers.methodReturnType}")
                .toD("dataformat:json-jackson:unmarshal?contextPath=${headers.methodReturnType}")
//                .unmarshal().avro(simple("${headers.methodReturnType}", String.class).toString())
//                .end()
                .process(exchange -> {
                    Message message = exchange.getIn();
                    log.info(message.getHeaders().toString());
                    log.info(message.getBody().toString());
                })
        ;
    }
}
