package io.ttyys.micrc.annotations.technology.integration;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface MessageProducer {
    String id() default "";
    String messagePublishEndpoint();
}
