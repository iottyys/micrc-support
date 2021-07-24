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

                .choice()
                .when(header("methodParameterType").isNotNull())
                .marshal().json()
                .end()

                .setBody(simple("${body}"))
                .to("direct:{{endpoint}}")

                .choice()
                .when(header("CamelJacksonUnmarshalType").isNotNull())
                // avro转换必须实现接口GenericContainer中的getSchema方法，所以后期通过avro schema自动生成的类可以这么转换， 暂时只能用json进行转换
                .to("dataformat:json-jackson:unmarshal?allow-unmarshall-type=true")
                .end()
        ;
    }
}
