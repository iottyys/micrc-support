package io.ttyys.micrc.sad.gradle.plugin.schema;

import org.gradle.internal.impldep.com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * 常量接口
 */
public interface Constants {
    String group = "idl2protocol design";

    char point = '.';

    String idlExtension = "avdl";
    String protocolExtension = "avpr";
    String querySuffix = "QueryController";
    String nameKey = "name";
    String namespaceKey = "namespace";
    String typesKey = "types";
    String javaAnnotationKey = "javaAnnotation";

    String projectJsonFileName = "project-config.json";
    String moduleJsonFileName = "module-config.json";
    String packagePrefixKey = "packagePrefix";
    Map<String, String> map = ImmutableMap.<String, String>builder()
            .put("api", "infrastructure.api.rpc")
            .put("queryDto", "presentation.%s.model")
            .put("dto", "application.dto")
            .build();
}
