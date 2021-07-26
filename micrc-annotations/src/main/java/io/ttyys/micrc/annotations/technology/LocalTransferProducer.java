package io.ttyys.micrc.annotations.technology;

import java.lang.annotation.*;
import org.springframework.core.annotation.AliasFor;

/**
 * 本地调用生产者(发起调用)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface LocalTransferProducer {
    /**
     * 调用端点
     * @return 端点uri
     */
    @AliasFor("value")
    String endpoint() default "";

    @AliasFor("endpoint")
    String value() default "";
}
