package io.ttyys.micrc.annotations.runtime;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ApiLogic {
    /**
     * 目标服务bean名称
     */
    String serviceName();

    /**
     * 目标方法
     * 方法名
     */
    String methodName() default "execute";

    /**
     * 目标参数转换bean名称
     */
    String mappingBean();

    /**
     * 目标参数转换对应方法
     */
    String mappingMethod() default "dtoToCommand";
}
