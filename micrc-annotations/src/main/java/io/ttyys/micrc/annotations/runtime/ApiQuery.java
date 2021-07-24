package io.ttyys.micrc.annotations.runtime;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ApiQuery {
    /**
     * 目标服务bean名称
     */
    String serviceName();

    /**
     * 目标方法
     * 方法名
     */
    String methodName() default "query";

    /**
     * 转换bean名称
     */
    String mappingBean() default "";

    /**
     * 返回数据转换对应方法
     */
    String mappingMethod() default "toData";
}
