package io.ttyys.micrc.annotations.technology;

/**
 * 远程调用生产者(发起调用)
 */
public @interface RpcTransferProducer {
    String endpoint();
    /**
     * @return 实现类名称--不包含包结构
     */
    String adapterClassName();
}
