package io.ttyys.micrc.codegen.gradle.plugin.common;

import org.apache.avro.Protocol;
import org.gradle.api.provider.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility method for working with Avro objects.
 */
public class AvroUtils {
    /**
     * The namespace separator.
     */
    public static final String NAMESPACE_SEPARATOR = ".";
    /**
     * The Unix separator.
     */
    public static final String UNIX_SEPARATOR = "/";

    /**
     * Not intended for instantiation.
     */
    private AvroUtils() {
    }

    /**
     * Assembles a file path based on the namespace and name of the provided {@link Protocol}.
     *
     * @param idlFile idl file
     * @return a file path
     */
    public static String assemblePath(File idlFile, String mainPath, boolean def,
                                      String moduleDir, Property<String> serviceIntegrationPath) {
        List<String> parts = new ArrayList<>();
//        parts.add(mainPath); // avro 主目录
        parts.add(serviceIntegrationPath.get()); // 功能设计目录
        String path = idlFile.getPath().split(mainPath)[1];
        String[] pathArr = path.split(UNIX_SEPARATOR);
        parts.add(moduleDir == null ? pathArr[2] : moduleDir);  // 模块目录
        parts.add(pathArr[3]);  // 协议请求类别  同步请求  异步消息
        RequestEnum requestEnum = RequestEnum.valueOf(pathArr[3].toUpperCase());
        parts.add(def ? requestEnum.getDefDirName() : requestEnum.getTransferDirName());  // 定义协议 / 调用监听
        parts.add(idlFile.getName().replace(Constants.IDL_EXTENSION, Constants.PROTOCOL_EXTENSION));
        return String.join(UNIX_SEPARATOR, parts);
    }
}
