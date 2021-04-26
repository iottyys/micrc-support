package io.ttyys.micrc.annotations.logic;

/**
 * 实现逻辑委托调用其他用例
 */
public @interface LogicDelegate {
    /**
     * @return 委托指向的应用
     */
    String application();

    /**
     * @return 委托执行的用例
     */
    String useCase();

    /**
     * @return 进委托的参数转换配置
     */
    String paramCovertXml();

    /**
     * @return 委托返回值的转换配置
     */
    String resultCovertXml();
}
