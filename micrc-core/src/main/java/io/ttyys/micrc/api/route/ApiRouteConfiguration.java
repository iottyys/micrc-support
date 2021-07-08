package io.ttyys.micrc.api.route;

import io.ttyys.micrc.api.ApiAspect;
import io.ttyys.micrc.api.common.dto.Result;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

public class ApiRouteConfiguration extends RouteBuilder {

    public static final String ROUTE_TMPL_API_RPC = ApiRouteConfiguration.class.getName() + ".apiRPC";

    @Override
    public void configure() {
        routeTemplate(ROUTE_TMPL_API_RPC)
                .templateParameter("id", null, "id")
                .templateParameter("targetParamMappingBean", null, "目标参数转换bean名称")
                .templateParameter("targetParamMappingMethod", null, "目标参数转换对应方法")
                .templateParameter("targetService", null, "目标服务bean名称")
                .templateParameter("targetMethod", null, "目标方法")
                .templateParameter("returnDataMappingBean", null, "返回数据转换bean名称")
                .templateParameter("returnDataMappingMethod", null, "返回数据转换对应方法")
                .from(ApiAspect.POINT + "-{{id}}")
                .to("bean-validator://ValidationProviderResolverTest?validationProviderResolver=#validationProviderResolver")
                .setExchangePattern(ExchangePattern.InOnly)
                .process(exchange -> {
                    Message message = exchange.getIn();
                    message.setHeader("sourceParam", message.getBody());
                })
                .to("bean:{{targetParamMappingBean}}?method={{targetParamMappingMethod}}")
                .to("bean:{{targetService}}?method={{targetMethod}}")
                .to("bean:{{returnDataMappingBean}}?method={{returnDataMappingMethod}}")
                .to("bean-validator://ValidationProviderResolverTest?validationProviderResolver=#validationProviderResolver")
                .to("log:" + getClass().getName()
                        + "?showAll=true&multiline=true&level=DEBUG");
        /*from(ApiAspect.POINT)
                .to("bean-validator://ValidationProviderResolverTest?validationProviderResolver=#validationProviderResolver")
                .setExchangePattern(ExchangePattern.InOnly)
                .process(exchange -> {
                    Message message = exchange.getIn();
                    message.setHeader("sourceParam", message.getBody());
                })
                .to("bean:personMapperImpl?method=in2Command")
                .to("bean:demoService?method=exec")
                .to("bean:personMapperImpl?method=command2Out")
                .to("bean-validator://ValidationProviderResolverTest?validationProviderResolver=#validationProviderResolver")
                .to("log:" + getClass().getName()
                        + "?showAll=true&multiline=true&level=DEBUG");*/

    }

    @Data
    @SuperBuilder
    public static class ApiDefinition {
        private String templateId;
        /**
         * id
         */
        private String id;
        /**
         * 目标参数转换bean名称
         */
        private String targetParamMappingBean;
        /**
         * 目标参数转换对应方法
         */
        private String targetParamMappingMethod;
        /**
         * 目标服务
         */
        private String targetService;
        /**
         * 目标方法
         */
        private String targetMethod;
        /**
         * 返回数据转换bean名称
         */
        private String returnDataMappingBean;
        /**
         * 返回数据转换对应方法
         */
        private String returnDataMappingMethod;
    }
}
