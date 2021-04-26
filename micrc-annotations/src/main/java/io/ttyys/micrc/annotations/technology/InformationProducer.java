package io.ttyys.micrc.annotations.technology;

/**
 * 消息生产者(发送)
 */
public @interface InformationProducer {
    String endpoint();
    /**
     * @return 实现类名称--不包含包结构
     */
    String adapterClassName();
}
