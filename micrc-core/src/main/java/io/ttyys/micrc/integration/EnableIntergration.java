package io.ttyys.micrc.integration;

import io.ttyys.micrc.integration.local.springboot.EnableLocalMessageSupport;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 集成调用开启注解
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/4/25 7:34 下午
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableLocalMessageSupport
public @interface EnableIntergration {

    String[] servicePackages() default {};
}
