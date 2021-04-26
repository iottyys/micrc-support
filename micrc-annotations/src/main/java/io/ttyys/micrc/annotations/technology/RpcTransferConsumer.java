package io.ttyys.micrc.annotations.technology;

/**
 * 远程调用消费者(被调用)
 */
public @interface RpcTransferConsumer {
    String endpoint();
    /**
     * @return 实现类名称--不包含包结构
     */
    String adapterClassName();
}
