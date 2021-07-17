package io.ttyys.micrc.api;

import com.google.common.collect.ImmutableMap;
import io.ttyys.micrc.annotations.runtime.ApiQuery;
import io.ttyys.micrc.api.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Collection;
import java.util.Map;


/**
 * 系统日志，切面处理类
 *
 * @Author scott
 * @email jeecgos@163.com
 * @Date 2018年1月14日
 */
@Aspect
@Slf4j
public class ApiQueryAspect {
    public static final String POINT = "direct:io.ttyys.micrc.api.route.ApiRouteConfiguration";

    @EndpointInject(property = "point")
    private ProducerTemplate producer;

    public String getPoint() {
        return POINT;
    }

    @Pointcut("@annotation(io.ttyys.micrc.annotations.runtime.ApiQuery)")
    public void apiPointCut() {
    }

    @Around("apiPointCut() && @annotation(apiQuery)")
    public Object around(ProceedingJoinPoint point, ApiQuery apiQuery) {
        long beginTime = System.currentTimeMillis();
        //执行方法
//        Object result = point.proceed();
        //执行时长(毫秒)

        Object body = this.handleRequestBody(point);
        Map<String, Object> headers = ImmutableMap.<String, Object>builder()
                .put("id", String.format("%s.%s", apiQuery.serviceName(), apiQuery.methodName()))
                .put("serviceName", apiQuery.serviceName())
                .put("methodName", apiQuery.methodName())
                .put("mappingBean", apiQuery.mappingBean())
                .put("mappingMethod", apiQuery.mappingMethod())
                .build();
        long time = System.currentTimeMillis() - beginTime;
        log.info("query times: {}", time);

        try {
            Endpoint endpoint = getCurrentEndpoint(apiQuery);
            Object result = this.producer.requestBodyAndHeaders(endpoint, body, headers);
            return Result.OK(result);
        } catch (Exception e) {
            return Result.error(e.getCause().getMessage());
        }
    }

    private Endpoint getCurrentEndpoint(ApiQuery apiQuery) {
        Collection<Endpoint> endpoints = this.producer.getCamelContext().getEndpoints();
        for (Endpoint endpoint : endpoints) {
            if (endpoint.getEndpointUri().endsWith(String.format("%s.%s", apiQuery.serviceName(), apiQuery.methodName()))) {
                return endpoint;
            }
        }
        return null;
    }

    protected Object handleRequestBody(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 1) {
            throw new IllegalArgumentException("support at most one argument");
        }
        return args.length > 0 ? args[0] != null ? args[0] : new Object() : new Object();
    }
}
