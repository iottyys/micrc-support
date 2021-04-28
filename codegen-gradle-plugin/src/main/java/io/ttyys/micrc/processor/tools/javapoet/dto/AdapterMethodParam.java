package io.ttyys.micrc.processor.tools.javapoet.dto;

import com.squareup.javapoet.TypeName;

/**
 * 适配器生成时需要的方法参数配置
 */
public class AdapterMethodParam {
    private TypeName clazz;
    private String name;

    public AdapterMethodParam(TypeName clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public TypeName getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }
}
