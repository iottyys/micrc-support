package io.ttyys.micrc.annotations.technology;

import java.lang.annotation.*;

/**
 * 本地调用消费者(被调用)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface LocalTransferConsumer {
    String endpoint();

    /**
     * @return 实现类名称--不包含包结构
     */
    String adapterClassName();
}
