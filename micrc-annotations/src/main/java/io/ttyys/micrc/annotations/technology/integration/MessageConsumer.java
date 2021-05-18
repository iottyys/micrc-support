package io.ttyys.micrc.annotations.technology.integration;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface MessageConsumer {
    String id() default "";
    String topicName();
    String subscriptionName();
    String adapterName();
}
