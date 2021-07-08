package io.ttyys.micrc.api;

import io.ttyys.micrc.annotations.runtime.ApiLogic;
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

        ApiLogicScanner apiLogicScanner = new ApiLogicScanner(registry, source);
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

class ApiLogicScanner extends ClassPathBeanDefinitionScanner {

    private static final AtomicInteger INDEX = new AtomicInteger();
    private final ApiRouteTemplateParameterSource sourceDefinition;

    public ApiLogicScanner(BeanDefinitionRegistry registry,
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
                ApiLogic apiLogic = method.getAnnotation(ApiLogic.class);
                if (apiLogic != null) {
                    sourceDefinition.addParameter(
                            routeId(apiLogic),
                            ApiRouteConfiguration.ApiDefinition.builder()
                                    .templateId(ApiRouteConfiguration.ROUTE_TMPL_API_RPC)
                                    .id(apiLogic.id())
                                    .targetParamMappingBean(apiLogic.targetParamMappingBean())
                                    .targetParamMappingMethod(apiLogic.targetParamMappingMethod())
                                    .targetService(apiLogic.targetService())
                                    .targetMethod(apiLogic.targetMethod())
                                    .returnDataMappingBean(apiLogic.returnDataMappingBean())
                                    .returnDataMappingMethod(apiLogic.returnDataMappingMethod())
                                    .build());
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
        String routeId = apiLogic.id();
        if (!StringUtils.hasText(routeId)) {
            routeId = String.valueOf(INDEX.getAndIncrement());
        }
        return ApiRouteConfiguration.ROUTE_TMPL_API_RPC + "-" + routeId;
    }
}

