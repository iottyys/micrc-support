package io.ttyys.micrc.annotations.technology;

/**
 * 本地调用生产者(发起调用)
 */
public @interface LocalTransferProducer {
    String endpoint();
    /**
     * @return 实现类名称--不包含包结构
     */
    String adapterClassName();
}
