package io.ttyys.micrc.integration.local.camel;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;

/**
 * 本地消息路由注册
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/5/3 1:48 下午
 */
@Slf4j
public class LocalMessageRouterBuilder implements ApplicationRunner {

    @Autowired
    private LocalConsumerRoutesInfo localConsumerRoutesInfo;

    @Autowired
    private LocalProducerRoutesInfo localProducerRoutesInfo;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 通过路由模版以及携带信息构造消路由
        localConsumerRoutesInfo.getRoutesInfo().stream().forEach(routeInfo ->
                TemplatedRouteBuilder
                        .builder(camelContext, "localConsumerRouteTemplate")
                        .parameters(routeInfo)
                        .add()
        );
        // 通过路由模版以及携带信息构造生产路由
        localProducerRoutesInfo.getRoutesInfo().stream().forEach(routeInfo ->
                TemplatedRouteBuilder
                        .builder(camelContext, "localProducerRouteTemplate")
                        .parameters(routeInfo)
                        .add()
        );
        log.info("producer and consumer router build complete......");
    }
}
