package io.ttyys.micrc.api;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({ApiAutoConfiguration.class, ClassPathApiScannerRegistrar.class})
public @interface EnableApi {
    String[] basePackages() default {};
}
