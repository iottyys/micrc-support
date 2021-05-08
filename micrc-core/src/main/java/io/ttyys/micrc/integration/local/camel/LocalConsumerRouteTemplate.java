package io.ttyys.micrc.integration.local.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

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
                .from("direct:{{endpoint}}")
                .routeId("consumer-{{adapterClassName}}")
                .unmarshal().json(JsonLibrary.Jackson)
                // .bean("#{{adapterClassName}}", "-D${headers.methodName}(${body})")
                .setBody(simple("${body}"))
                .log("${body}")
                .log("${headers.methodName}")
                .toD("bean:{{adapterClassName}}?method=${headers.methodName}");
    }
}
