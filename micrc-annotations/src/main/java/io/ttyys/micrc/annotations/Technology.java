package io.ttyys.micrc.annotations;

/**
 * 技术
 */
public @interface Technology {
    String value() default "";

    ImplType implType() default ImplType.CUSTOM;



    /**
     * 实现方式: 编写实现/自动实现
     */
    enum ImplType {CUSTOM, AUTO}
}
