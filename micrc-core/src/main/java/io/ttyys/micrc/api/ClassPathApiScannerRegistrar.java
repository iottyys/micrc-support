package io.ttyys.micrc.api;

import io.ttyys.micrc.annotations.runtime.ApiLogic;
import io.ttyys.micrc.annotations.runtime.ApiQuery;
import io.ttyys.micrc.api.route.ApiRouteConfiguration;
import io.ttyys.micrc.api.route.ApiRouteTemplateParameterSource;
import lombok.SneakyThrows;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClassPathApiScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @SuppressWarnings("unchecked")
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry,
                                        BeanNameGenerator importBeanNameGenerator) {
        AnnotationAttributes attributes = AnnotationAttributes
                .fromMap(importingClassMetadata.getAnnotationAttributes(EnableApi.class.getName()));
        assert attributes != null;
        String[] basePackages = attributes.getStringArray("basePackages");
        if (basePackages.length == 0 && importingClassMetadata instanceof StandardAnnotationMetadata) {
            basePackages = new String[]{((StandardAnnotationMetadata) importingClassMetadata)
                    .getIntrospectedClass().getPackage().getName()};
        }
        if (basePackages.length == 0) {
            return;
        }
        ApiRouteTemplateParameterSource source = new ApiRouteTemplateParameterSource();

        ApiScanner apiLogicScanner = new ApiScanner(registry, source);
        apiLogicScanner.setResourceLoader(resourceLoader);
        apiLogicScanner.doScan(basePackages);
        // registering
        BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(
                        (Class<ApiRouteTemplateParameterSource>) source.getClass(),
                        () -> source)
                .getRawBeanDefinition();
        // remove route template source bean registered from ApiAutoConfiguration
        registry.removeBeanDefinition("fakeApiRouteTemplateParameterSource");
        registry.registerBeanDefinition(importBeanNameGenerator.generateBeanName(beanDefinition, registry),
                beanDefinition);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}

class ApiScanner extends ClassPathBeanDefinitionScanner {

    private static final AtomicInteger INDEX = new AtomicInteger();
    private final ApiRouteTemplateParameterSource sourceDefinition;

    public ApiScanner(BeanDefinitionRegistry registry,
                           ApiRouteTemplateParameterSource source) {
        super(registry, false);
        this.sourceDefinition = source;
    }

    public boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        return true;
    }

    @SneakyThrows
    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        this.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        for (BeanDefinitionHolder holder : holders) {
            GenericBeanDefinition beanDefinition = (GenericBeanDefinition) holder.getBeanDefinition();
            beanDefinition.resolveBeanClass(Thread.currentThread().getContextClassLoader());

            ReflectionUtils.doWithLocalMethods(beanDefinition.getBeanClass(), method -> {
                ApiRouteConfiguration.AbstractApiDefinition definition;
                ApiLogic apiLogic = method.getAnnotation(ApiLogic.class);
                if (apiLogic != null) {
                    definition = ApiRouteConfiguration.ApiLogicDefinition
                            .builder()
                            .templateId(ApiRouteConfiguration.ROUTE_TMPL_API_LOGIC_RPC)
                            .id(String.format("%s.%s", apiLogic.serviceName(), apiLogic.methodName()))
                            .serviceName(apiLogic.serviceName())
                            .methodName(apiLogic.methodName())
                            .mappingBean(apiLogic.mappingBean())
                            .mappingMethod(apiLogic.mappingMethod())
                            .build();
                    sourceDefinition.addParameter(routeId(apiLogic), definition);
                }
                ApiQuery apiQuery = method.getAnnotation(ApiQuery.class);
                if (apiQuery != null) {
                    definition = ApiRouteConfiguration.ApiQueryDefinition
                            .builder()
                            .templateId(ApiRouteConfiguration.ROUTE_TMPL_API_QUERY_RPC)
                            .id(String.format("%s.%s", apiQuery.serviceName(), apiQuery.methodName()))
                            .serviceName(apiQuery.serviceName())
                            .methodName(apiQuery.methodName())
                            .mappingBean(apiQuery.mappingBean())
                            .mappingMethod(apiQuery.mappingMethod())
                            .build();
                    sourceDefinition.addParameter(routeId(apiQuery), definition);
                }

            });
        }
        holders.clear();
        return holders;
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry routersInfo) {
        // nothing to do. leave it out.
    }

    private String routeId(ApiLogic apiLogic) {
        String routeId = String.format("%s.%s", apiLogic.serviceName(), apiLogic.methodName());
        if (!StringUtils.hasText(routeId)) {
            routeId = String.valueOf(INDEX.getAndIncrement());
        }
        return ApiRouteConfiguration.ROUTE_TMPL_API_LOGIC_RPC + "-" + routeId;
    }

    private String routeId(ApiQuery apiQuery) {
        String routeId = String.format("%s.%s", apiQuery.serviceName(), apiQuery.methodName());
        if (!StringUtils.hasText(routeId)) {
            routeId = String.valueOf(INDEX.getAndIncrement());
        }
        return ApiRouteConfiguration.ROUTE_TMPL_API_QUERY_RPC + "-" + routeId;
    }
}

