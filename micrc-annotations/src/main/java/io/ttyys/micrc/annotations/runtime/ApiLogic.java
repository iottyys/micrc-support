package io.ttyys.micrc.annotations.runtime;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ApiLogic {
    /**
     * id
     */
    String id();

    /**
     * 目标参数转换bean名称
     */
    String targetParamMappingBean();
    /**
     * 目标参数转换对应方法
     */
    String targetParamMappingMethod();

    /**
     * 目标服务bean名称
     */
    String targetService();

    /**
     * 目标方法
     * 方法名 非必选（调用应用服务的时候不需要，查询服务的时候必须）(目标服务中有一个方法的时候不需要, 多个的时候必须)
     */
    String targetMethod() default "exec";

    /**
     * 返回数据转换bean名称
     */
    String returnDataMappingBean();
    /**
     * 返回数据转换对应方法
     */
    String returnDataMappingMethod();

}
