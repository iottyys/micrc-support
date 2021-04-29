package io.ttyys.micrc.integration.local.camel;

import org.apache.camel.builder.RouteBuilder;

/**
 * 本地消息生产端路由模版
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/4/28 6:10 下午
 */
public class LocalProducerRouteTemplate extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        routeTemplate("localProducerRouteTemplate")
                .templateParameter("endpoint")
                .templateParameter("adapterClassName")
                .templateParameter("method")
                .from("{{endpoint}}{{method}}").marshal()
                .avro()// 这一步要拿Avro里的那个数据传输对象的class
                .setBody(simple("${body}"))
                .bean("#{{adapterClassName}}", "-D${method}")
                .log("${body}");
    }
}
