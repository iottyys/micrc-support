package io.ttyys.micrc.integration.local.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地消息消费端路由模版
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/4/28 6:10 下午
 */
@Configuration
public class LocalConsumerRouteTemplate extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        routeTemplate("localConsumerRouteTemplate")
                .templateParameter("endpoint")
                .templateParameter("adapterClassName")
                .from("direct:{{endpoint}}").unmarshal()
                .avro()// 这一步要拿Avro里的那个数据传输对象的class
                .setBody(simple("${body}"))
                .bean("#{{adapterClassName}}?method=${method}")
                //.bean("#{{adapterClassName}}", "-D${method}")
                .log("${body}");
//        routeTemplate("localConsumerRouteTemplate")
//                .templateParameter("endpoint")
//                .templateParameter("adapterClassName")
//                .from("direct:{{endpoint}}")
//                .setBody(simple("${body}"))
//                .log("${body}");
    }

    @Autowired
    private LocalConsumerRoutesInfo localConsumerRoutesInfo;

    @Bean
    public CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // 通过路由模版以及携带信息构造路由
                localConsumerRoutesInfo.getRoutesInfo().stream().forEach(routeInfo ->
                        TemplatedRouteBuilder
                                .builder(camelContext, "localConsumerRouteTemplate")
                                .parameters(routeInfo)
                                .add()
                );
            }
        };
    }
}
