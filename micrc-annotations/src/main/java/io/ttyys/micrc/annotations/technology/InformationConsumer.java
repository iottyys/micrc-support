package io.ttyys.micrc.annotations.technology;

/**
 * 消息消费者(监听)
 */
public @interface InformationConsumer {
    String endpoint();

    /**
     * @return 实现类名称--不包含包结构
     */
    String adapterClassName();
}
