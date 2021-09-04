/**
 * Copyright © 2013-2019 Commerce Technologies, LLC.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ttyys.micrc.sad.gradle.plugin.schema.task;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.ttyys.micrc.sad.gradle.plugin.common.ProjectUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileExtensionSpec;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileUtils;
import io.ttyys.micrc.sad.gradle.plugin.schema.Constants;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.NotSpec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 设计结构说明 Structure
 * 设计源 src/main/avro
 * ├- project-config.json -- 项目相关的配置（项目前缀包名/部署方式/各部分的技术定义/等）
 * └- 模块目录
 * ├- module-config.json -- 模块相关配置（模块前缀包名/本地消息调用关系/等）
 * ├- api(controller)
 * ├- application(service应用/查询服务)
 * ├- domain
 * ├- local
 * <p>
 * 编译目录 build/avpr
 */
public class DealProtocolStructureTask extends OutputDirTask {
    private final Project project;

    private File protocolDirectory;
    /**
     * 类型Map<原始包名.schemaName/protocolName,调整后包名.schemaName/protocolName>
     */
    private final Map<String, String> typePkgMap;

    @Inject
    public DealProtocolStructureTask() {
        project = getProject();
        typePkgMap = new HashMap<>(0);
    }

    @TaskAction
    protected void process() {
        getLogger().info("Found {} files", getSource().getFiles().size());
        failOnUnsupportedFiles();
        processFiles();
    }

    private void failOnUnsupportedFiles() {
        FileCollection unsupportedFiles = filterSources(new NotSpec<>(new FileExtensionSpec(Constants.protocolExtension, Constants.schemaExtension)));
        if (!unsupportedFiles.isEmpty()) {
            throw new GradleException(
                    String.format("Unsupported file extension for the following files: %s", unsupportedFiles));
        }
    }

    private void processFiles() {
        int processedFileCount = 0;
        SourceSet sourceSet = ProjectUtils.getMainSourceSet(project);
        File srcDir = ProjectUtils.getAvroSourceDir(project, sourceSet);
        String projectPkg = getProjectPackage(srcDir);

        processSchemaFile(srcDir, projectPkg);
        processedFileCount += typePkgMap.size();
        for (File sourceFile : filterSources(new FileExtensionSpec(Constants.protocolExtension))) {
            // 调整完成协议的包结构之后，通过上面保存的map将所有存在引用的类型应用处也进行调整
            String modulePkg = getModulePkg(projectPkg, sourceFile, srcDir);
            processProtocolFile(sourceFile, modulePkg);
            processedFileCount++;
        }
        setDidWork(processedFileCount > 0);
    }

    private void processSchemaFile(File srcDir, String projectPkg) {
        try {
            for (File sourceFile : filterSources(new FileExtensionSpec(Constants.schemaExtension))) {
                Schema schema = new Schema.Parser().parse(sourceFile);
                sourceFile.deleteOnExit();

                String json = schema.toString();
                JSONObject jsonObject = JSONObject.parseObject(json);
                // 调整协议类的包结构
                String fileNamespace = schema.getNamespace();
                String modulePkg = getModulePkg(projectPkg, sourceFile, srcDir);
                checkAndSetSchemaFieldTypeNamespace(jsonObject, modulePkg, fileNamespace);
                FileUtils.writeJsonFile(sourceFile, new Schema.Parser().parse(jsonObject.toJSONString()).toString(true));
            }
            getLogger().debug("schema deal structure finish.");
        } catch (IOException ex) {
            throw new GradleException("schema deal structure error.", ex);
        }
    }

    private void designPkg(String modulePkg, JSONObject jsonObj, JSONObject jsonMainObj) {
        // 设计当前传进来的jsonObj的namespace，并将其与类全名的旧名称与新名称映射关系加入到typePkgMap
        String curNamespace = jsonObj.getString(Constants.namespaceKey);
        String newNamespace;
        String curName = jsonObj.getString(Constants.nameKey);
        if (typePkgMap.containsKey(curNamespace)) {
            newNamespace = typePkgMap.get(curNamespace);
        } else {
            newNamespace = modulePkg + Constants.point + Constants.map.getOrDefault(curNamespace, curNamespace);
            typePkgMap.put(curNamespace, newNamespace);
        }
        jsonObj.put(Constants.namespaceKey, newNamespace);
        String key = curNamespace + Constants.point + curName;
        typePkgMap.put(key, newNamespace + Constants.point + curName);

        if (jsonObj.containsKey(Constants.fieldsKey)) {
            JSONArray fieldsJsonArray = jsonObj.getJSONArray(Constants.fieldsKey);
            for (int j = 0, jLen = fieldsJsonArray.size(); j < jLen; j++) {
                JSONObject fieldJsonObject = fieldsJsonArray.getJSONObject(j);
                Object typeObject = fieldJsonObject.get(Constants.typeKey);
                if (typeObject instanceof JSONObject) {
                    JSONObject typeJsonObj = (JSONObject) typeObject;
                    Object o = typeJsonObj.get(Constants.typeKey);
                    if (o instanceof String) {
                        String typeStr = (String) o;
                        if (Schema.Type.RECORD.getName().equals(typeStr)) {
                        } else if (Schema.Type.ARRAY.getName().equals(typeStr)) {
                        }
                    }
                }
            }
        }
    }

    private void checkAndSetSchemaFieldTypeNamespace(JSONObject jsonObject, String modulePkg, String fileNamespace) {
        String curNamespace = jsonObject.getString(Constants.namespaceKey);
        String curName = jsonObject.getString(Constants.nameKey);
        String key = curNamespace + Constants.point + curName;
        String schemaNamespace = modulePkg + Constants.point + Constants.map.getOrDefault(curNamespace, curNamespace);
        jsonObject.put(Constants.namespaceKey, schemaNamespace);
        typePkgMap.put(key, schemaNamespace + Constants.point + curName);
        if (jsonObject.containsKey(Constants.fieldsKey)) {
            JSONArray fieldsJsonArray = jsonObject.getJSONArray(Constants.fieldsKey);
            for (int j = 0, jLen = fieldsJsonArray.size(); j < jLen; j++) {
                JSONObject fieldJsonObject = fieldsJsonArray.getJSONObject(j);
                Object typeObject = fieldJsonObject.get(Constants.typeKey);
                setRecordNamespace(typeObject, modulePkg, schemaNamespace, fileNamespace);
            }
        }
    }

    private void setRecordNamespace(Object typeObject, String modulePkg, String namespace, String fileNamespace) {
        if (typeObject instanceof JSONObject) {
            JSONObject typeJsonObject = (JSONObject) typeObject;
            String typeType = typeJsonObject.getString(Constants.typeKey);
            if ("record".equals(typeType)) {
                String curTypeNamespace = typeJsonObject.getString(Constants.namespaceKey);
                if (typeJsonObject.containsKey(Constants.namespaceKey)) {
                    String typeNamespace = modulePkg + Constants.point
                            + Constants.map.getOrDefault(curTypeNamespace, curTypeNamespace);
                    typeJsonObject.put(Constants.namespaceKey, typeNamespace);
                } else {
                    typeJsonObject.put(Constants.namespaceKey, namespace);
                }
            } else if ("array".equals(typeType)) {
                Object arrayItemObject = typeJsonObject.get(Constants.itemsKey);
                setRecordNamespace(arrayItemObject, modulePkg, namespace, fileNamespace);
            }
            checkAndSetSchemaFieldTypeNamespace(typeJsonObject, modulePkg, fileNamespace);
        }
    }

    private void processProtocolFile(File sourceFile, String modulePkg) {
        try {
            Protocol protocol = Protocol.parse(sourceFile);
            sourceFile.deleteOnExit();

            String protoJson = protocol.toString();
            JSONObject jsonObject = JSONObject.parseObject(protoJson);
            // 调整协议类的包结构
            String fileNamespace = protocol.getNamespace();
            checkAndSetSchemaFieldTypeNamespace(jsonObject, modulePkg, fileNamespace);

            loadTypePkg(modulePkg, protocol);

            // (定义类)类型属性  --  注意嵌套类型定义
            checkAndReplaceValue(typePkgMap, jsonObject, Constants.typesKey);
            JSONArray typesJsonArray = jsonObject.getJSONArray(Constants.typesKey);
            for (int i = 0, len = typesJsonArray.size(); i < len; i++) {
                JSONObject typeJsonObject = typesJsonArray.getJSONObject(i);
                String typeAllName = typePkgMap.get(typeJsonObject.getString(Constants.namespaceKey)
                        + Constants.point + typeJsonObject.getString(Constants.nameKey));
                typeJsonObject.put(Constants.namespaceKey, typeAllName);
            }
            // 入参返参

            FileUtils.writeJsonFile(sourceFile, Protocol.parse(jsonObject.toJSONString()).toString(true));
            getLogger().debug("协议调整结构完成 {}", sourceFile.getPath());
        } catch (IOException ex) {
            throw new GradleException(String.format("Failed to process protocol definition file %s", sourceFile), ex);
        }
    }

    /**
     * 如果协议中定义了参数的类型，对类型的包结构进行调整
     *
     * @param baseNamespace 基础包
     * @param protocol      协议
     */
    private void loadTypePkg(String baseNamespace, Protocol protocol) {
        String schemaNamespace;

        Collection<Schema> schemaColl = protocol.getTypes();
        for (Schema schema : schemaColl) {
            String curNamespace = schema.getNamespace();
            String key = curNamespace + Constants.point + schema.getName();
            if (curNamespace != null && curNamespace.equals(protocol.getNamespace())) {
                String schemaType = schema.getName().toLowerCase().endsWith("dto") ? "dto" : "vo";
                if (protocol.getName().contains(Constants.queryKeyword)) {
                    // 查询api Controller
                    schemaNamespace = baseNamespace + Constants.point
                            + String.format(
                            Constants.map.getOrDefault("query" + schemaType, schemaType),
                            protocol.getName().replace(Constants.queryKeyword, "").toLowerCase()
                    );
                } else {
                    // 应用api Controller
                    schemaNamespace = baseNamespace + Constants.point + Constants.map.getOrDefault(schemaType, schemaType);
                }
            } else {
                schemaNamespace = baseNamespace + Constants.point + Constants.map.getOrDefault(curNamespace, curNamespace);
            }
            typePkgMap.put(key, schemaNamespace + Constants.point + schema.getName());
        }
    }

    private void checkAndSetType(Map<String, String> typeNameMap, JSONArray jsonArray, String key) {
        for (int j = 0, jLen = jsonArray.size(); j < jLen; j++) {
            JSONObject jsonObject = jsonArray.getJSONObject(j);
            checkAndReplaceValue(typeNameMap, jsonObject, key);
        }
    }

    private void checkAndReplaceValue(Map<String, String> typePkgMap, JSONObject json, String key) {
        Object type = json.get(key);
        if (type instanceof JSONArray) {
            JSONArray jsonArray = json.getJSONArray(key);
            for (int j = 0, jLen = jsonArray.size(); j < jLen; j++) {
                JSONObject jsonObject = jsonArray.getJSONObject(j);
                String typeAllName = typePkgMap.get(jsonObject.getString(Constants.namespaceKey)
                        + Constants.point + jsonObject.getString(Constants.nameKey));
                jsonObject.put(Constants.namespaceKey, typeAllName);
                checkAndReplaceValue(typePkgMap, jsonObject, key);
            }
        } else if (type instanceof JSONObject) {
            JSONObject jsonObject = json.getJSONObject(key);
            checkAndReplaceValue(typePkgMap, jsonObject, "items");
        } else {
            if (typePkgMap.containsKey(json.getString(key))) {
                json.put(key, typePkgMap.get(json.getString(key)));
            }
        }
    }

    private String getProjectPackage(File srcDir) {
        String namespace;
        JSONObject projectConfigJson = loadJson(srcDir, Constants.projectJsonFileName);
        if (projectConfigJson.containsKey(Constants.packagePrefixKey)) {
            namespace = projectConfigJson.getString(Constants.packagePrefixKey);
        } else {
            namespace = "";
        }
        return namespace;
    }

    private String getModulePkg(String projectPkg, File sourceFile, File srcDir) {
        // 相对路径
        String relativePath = sourceFile.getParentFile().getAbsolutePath().replaceAll(
                protocolDirectory.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\"), "");
        String[] pathArr = relativePath.substring(1).split("\\".equals(File.separator) ? "\\\\" : File.separator); // 模块，架构功能
        File moduleDir = new File(srcDir.getAbsolutePath(), pathArr[0]);
        JSONObject moduleConfigJson = loadJson(moduleDir, Constants.moduleJsonFileName);
        // 构造结构信息
        return projectPkg + Constants.point + moduleConfigJson.getOrDefault(Constants.packagePrefixKey, pathArr[0]);
    }

    public JSONObject loadJson(File srcDir, String fileName) {
        String jsonStr = FileUtils.readJsonString(srcDir.getAbsolutePath() + File.separator + fileName);
        return JSONObject.parseObject(jsonStr);
    }

    public DealProtocolStructureTask setProtocolDirectory(File protocolDirectory) {
        this.protocolDirectory = protocolDirectory;
        return this;
    }
}
