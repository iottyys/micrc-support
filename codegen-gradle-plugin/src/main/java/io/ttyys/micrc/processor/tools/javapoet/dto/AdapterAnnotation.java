package io.ttyys.micrc.processor.tools.javapoet.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 适配器生成时需要的注解配置
 */
public class AdapterAnnotation {
    Class<?> clazz;
    Map<String, Object> params = new HashMap<>(0);

    public AdapterAnnotation(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
