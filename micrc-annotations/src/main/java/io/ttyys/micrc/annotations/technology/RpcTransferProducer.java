package io.ttyys.micrc.annotations.technology;

/**
 * 远程调用生产者(发起调用)
 */
public @interface RpcTransferProducer {
    String endpoint();
}
