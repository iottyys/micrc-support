package io.ttyys.micrc.sad.gradle.plugin.schema;

import java.util.HashMap;
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
    Map<String, String> map = new HashMap<String, String>() {
        {
            put("queryvo", "presentation.%s.model");
            put("api", "infrastructure.api.rpc");
            put("dto", "application.dto");
            put("vo", "application.vo");
            put("querydto", "presentation.%s.model");
        }
    };
}
