package io.ttyys.micrc.integration.local.camel;

import org.apache.camel.builder.RouteBuilder;

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
                .unmarshal().json()
                .setBody(simple("${body}"))
                // .bean("#{{adapterClassName}}", "-D${headers.methodName}(${body})")
                .bean("#{{adapterClassName}}", "-D${headers.methodName}")
                .log("${body}")
                .log("${headers.methodName}");
    }
}
