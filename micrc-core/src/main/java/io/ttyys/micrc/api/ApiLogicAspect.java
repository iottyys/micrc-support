package io.ttyys.micrc.api;

import com.google.common.collect.ImmutableMap;
import io.ttyys.micrc.annotations.runtime.ApiLogic;
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
public class ApiLogicAspect {
    public static final String POINT = "direct:io.ttyys.micrc.api.route.ApiRouteConfiguration";

    @EndpointInject(property = "point")
    private ProducerTemplate producer;

    public String getPoint() {
        return POINT;
    }

    @Pointcut("@annotation(io.ttyys.micrc.annotations.runtime.ApiLogic)")
    public void apiPointCut() {
    }

    @Around("apiPointCut() && @annotation(apiLogic)")
    public Object around(ProceedingJoinPoint point, ApiLogic apiLogic) {
        long beginTime = System.currentTimeMillis();
        //执行方法
//        Object result = point.proceed();
        //执行时长(毫秒)

        Object body = this.handleRequestBody(point);
        Map<String, Object> headers = ImmutableMap.<String, Object>builder()
                .put("id", String.format("%s.%s", apiLogic.serviceName(), apiLogic.methodName()))
                .put("serviceName", apiLogic.serviceName())
                .put("methodName", apiLogic.methodName())
                .put("mappingBean", apiLogic.mappingBean())
                .put("mappingMethod", apiLogic.mappingMethod())
                .build();
        long time = System.currentTimeMillis() - beginTime;
        log.info("execute times: {}", time);

        try {
            Endpoint endpoint = getCurrentEndpoint(apiLogic);
//            Object result =
            this.producer.requestBodyAndHeaders(endpoint, body, headers);
            return Result.OK();
        } catch (Exception e) {
            return Result.error(e.getCause().getMessage());
        }
    }

    private Endpoint getCurrentEndpoint(ApiLogic apiLogic) {
        Collection<Endpoint> endpoints = this.producer.getCamelContext().getEndpoints();
        for (Endpoint endpoint : endpoints) {
            if (endpoint.getEndpointUri().endsWith(String.format("%s.%s", apiLogic.serviceName(), apiLogic.methodName()))) {
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
