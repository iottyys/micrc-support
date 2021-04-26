package io.ttyys.micrc.processor.tools.javapoet.dto;

/**
 * 适配器生成时需要的方法参数配置
 */
public class AdapterMethodParam {
    private Class<?> clazz;
    private String name;

    public AdapterMethodParam(Class<?> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }
}
