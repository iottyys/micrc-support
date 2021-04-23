package io.ttyys.micrc.annotations;

/**
 * 实现类支持注解
 */
public @interface TypeMeta {
    /**
     * @return 使用技术 technology
     */
    String value() default "";

    /**
     * @return 映射注解
     */
    String mapping() default "";

    /**
     * @return 映射路径
     */
    String url() default "";
}
