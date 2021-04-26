package io.ttyys.micrc.annotations;
/*
 * 六边形结构包
 * application      应用
 *   └ service          业务模块
 * domain           领域
 *   ├ event            事件
 *   ├ model            对象
 *   └ repository       数据仓库
 * infrastructure   基础设施(领域外的各端口定义)
 *   ├ mybatis          mybatis的端口
 *   ├ messaged         监听消息端口
 *   ├ messaging        发送消息端口
 *   ├ api              Api端口
 *   ├ called           接口提供端口
 *   └ calling          调用接口端口
 */

/**
 * 结构
 */
public @interface Structure {
    /**
     * @return 相对于六边形架构根的接口包名
     */
    String interfacePkg();
    /**
     * @return 相对于六边形架构根的实现包名
     */
    String implPkg();
}
