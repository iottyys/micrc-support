package io.ttyys.micrc.annotations.logic;

/**
 * 实现逻辑集成
 */
public @interface LogicIntegration {
    /**
     * @return 集成配置
     */
    String xml();
}
