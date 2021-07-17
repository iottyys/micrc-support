package io.ttyys.micrc.api.route;

import io.ttyys.micrc.api.ApiLogicAspect;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

public class ApiRouteConfiguration extends RouteBuilder {

    public static final String ROUTE_TMPL_API_LOGIC_RPC = ApiRouteConfiguration.class.getName() + ".apiLogicRPC";
    public static final String ROUTE_TMPL_API_QUERY_RPC = ApiRouteConfiguration.class.getName() + ".apiQueryRPC";

    @Override
    public void configure() {
        routeTemplate(ROUTE_TMPL_API_LOGIC_RPC)
                .templateParameter("id", null, "id")
                .templateParameter("serviceName", null, "业务服务bean名称")
                .templateParameter("methodName", null, "业务方法")
                .templateParameter("mappingBean", null, "映射bean名称")
                .templateParameter("mappingMethod", null, "入参映射方法")
                .from(ApiLogicAspect.POINT + "-{{id}}")
                .to("bean-validator://ValidationProviderResolverTest?validationProviderResolver=#validationProviderResolver")
                .setExchangePattern(ExchangePattern.InOnly)
                .process(exchange -> {
                    Message message = exchange.getIn();
                    message.setHeader("sourceParam", message.getBody());
                })
                .to("bean:{{mappingBean}}?method={{mappingMethod}}")
                .to("bean:{{serviceName}}?method={{methodName}}")
                .to("log:" + getClass().getName() + "?showAll=true&multiline=true&level=DEBUG");

        routeTemplate(ROUTE_TMPL_API_QUERY_RPC)
                .templateParameter("id", null, "id")
                .templateParameter("serviceName", null, "业务查询服务bean名称")
                .templateParameter("methodName", null, "业务方法")
                .templateParameter("mappingBean", null, "映射bean名称")
                .templateParameter("mappingMethod", null, "返回值映射方法")
                .from(ApiLogicAspect.POINT + "-{{id}}")
                .setExchangePattern(ExchangePattern.InOnly)
                .process(exchange -> {
                    Message message = exchange.getIn();
                    message.setHeader("sourceParam", message.getBody());
                })
                .to("bean:{{serviceName}}?method={{methodName}}")
                .to("bean:{{mappingBean}}?method={{mappingMethod}}")
                .to("bean-validator://ValidationProviderResolverTest?validationProviderResolver=#validationProviderResolver")
                .to("log:" + getClass().getName() + "?showAll=true&multiline=true&level=DEBUG");
    }

    @Data
    @SuperBuilder
    public static abstract class AbstractApiDefinition {
        private String templateId;
        /**
         * id
         */
        private String id;
        /**
         * 业务服务bean名称
         */
        private String serviceName;
        /**
         * 业务方法
         */
        private String methodName;
        /**
         * 映射bean名称
         */
        private String mappingBean;
        /**
         * 映射方法
         */
        private String mappingMethod;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder
    public static class ApiLogicDefinition extends AbstractApiDefinition {
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder
    public static class ApiQueryDefinition extends AbstractApiDefinition {
    }
}
