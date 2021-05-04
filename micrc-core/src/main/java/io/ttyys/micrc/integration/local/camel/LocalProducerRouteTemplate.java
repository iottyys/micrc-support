package io.ttyys.micrc.integration.local.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

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
                .templateParameter("beanClassName")
                .templateParameter("adapterClassName")
                .from("direct:{{beanClassName}}")
                .routeId("producer-{{beanClassName}}")
                // FIXME 这里需要修改为avro转换
                // https://stackoverflow.com/questions/40756027/apache-camel-json-marshalling-to-pojo-java-bean
                // https://camel.apache.org/components/3.9.x/dataformats/json-johnzon-dataformat.html
                .marshal().json(JsonLibrary.Jackson)
                .setBody(simple("${body}"))
                .to("direct:{{endpoint}}");
    }
}
