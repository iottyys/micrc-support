package io.ttyys.micrc.integration.local.springboot;

import io.ttyys.micrc.integration.local.camel.LocalConsumerRouteTemplate;
import io.ttyys.micrc.integration.local.camel.LocalMessageRouterBuilder;
import io.ttyys.micrc.integration.local.camel.LocalProducerRouteTemplate;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 本地同进程消息开启注解
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/4/25 7:34 下午
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
        ClassPathLocalConsumerScannerRegistrar.class,
        LocalConsumerRouteTemplate.class,
        ClassPathLocalProducerScannerRegistrar.class,
        LocalProducerRouteTemplate.class,
        LocalMessageRouterBuilder.class
})
public @interface EnableLocalMessageSupport {

    String[] servicePackages() default {};
}
