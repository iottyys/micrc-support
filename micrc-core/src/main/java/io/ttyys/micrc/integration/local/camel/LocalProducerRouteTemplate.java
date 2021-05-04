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
        // 这个路由里是要将

        routeTemplate("localProducerRouteTemplate")
                .templateParameter("endpoint")
                .templateParameter("beanClassName")
                .templateParameter("adapterClassName")
                .from("direct:{{beanClassName}}")
                .routeId("producer-{{beanClassName}}")
                .marshal().json()
                .setBody(simple("${body}"))
                .to("direct:{{endpoint}}");
    }
}
