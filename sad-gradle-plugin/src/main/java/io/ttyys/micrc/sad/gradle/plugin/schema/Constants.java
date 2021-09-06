package io.ttyys.micrc.sad.gradle.plugin.schema;

import java.util.HashMap;
import java.util.Map;

/**
 * 常量接口
 */
public interface Constants {
    String group = "idl2protocol design";

    String point = ".";

    String idlExtension = "avdl";
    String protocolKey = "protocol";
    String protocolExtension = "avpr";
    String schemaExtension = "avsc";
    String schemaNumDelimiter = "__"; // 分隔符
    String queryKeyword = "Query";
    String nameKey = "name";
    String namespaceKey = "namespace";
    String fieldsKey = "fields";
    String typesKey = "types";
    String typeKey = "type";
    String itemsKey = "items";
    String messagesKey = "messages";
    String requestKey = "request";
    String responseKey = "response";
    String javaAnnotationKey = "javaAnnotation";
    String source = "source";

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
