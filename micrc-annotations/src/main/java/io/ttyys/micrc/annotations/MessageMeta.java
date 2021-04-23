package io.ttyys.micrc.annotations;

/**
 * 消息(实现方法)支持注解 主要是映射路径
 */
public @interface MessageMeta {
    ImplType value() default ImplType.CUSTOM;

    /**
     * @return 映射注解
     */
    String mapping() default "";

    /**
     * @return 映射路径
     */
    String url() default "";

    /**
     * 实现方式: 编写实现 / camel集成实现 / 委托代理实现
     */
    enum ImplType {CUSTOM, INTEGRATION, PROXY}
}
