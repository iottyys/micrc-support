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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static io.ttyys.micrc.sad.gradle.plugin.common.Constants.PROTOCOL_EXTENSION;

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
    private Project project;

    private File protocolDirectory;

    @Inject
    public DealProtocolStructureTask() {
        project = getProject();
    }

    @TaskAction
    protected void process() {
        getLogger().info("Found {} files", getSource().getFiles().size());
        failOnUnsupportedFiles();
        processFiles();
    }

    private void failOnUnsupportedFiles() {
        FileCollection unsupportedFiles = filterSources(new NotSpec<>(new FileExtensionSpec(PROTOCOL_EXTENSION)));
        if (!unsupportedFiles.isEmpty()) {
            throw new GradleException(
                    String.format("Unsupported file extension for the following files: %s", unsupportedFiles));
        }
    }

    private void processFiles() {
        int processedFileCount = 0;
        SourceSet sourceSet = ProjectUtils.getMainSourceSet(project);
        File srcDir = ProjectUtils.getAvroSourceDir(project, sourceSet);
        // TODO zhaowang
        for (File sourceFile : filterSources(new FileExtensionSpec(Constants.schemaExtension))) {
            // 先进行schema的结构调整，完成后，将前后的名称以map保存
        }
        for (File sourceFile : filterSources(new FileExtensionSpec(PROTOCOL_EXTENSION))) {
            // 调整完成协议的包结构之后，通过上面保存的map将所有存在引用的类型应用处也进行调整
            processProtocolFile(sourceFile, srcDir);
            processedFileCount++;
        }
        setDidWork(processedFileCount > 0);
    }

    private void processProtocolFile(File sourceFile, File srcDir) {
        try {
            String baseNamespace = dealNamespace(sourceFile, srcDir);
            Protocol protocol = Protocol.parse(sourceFile);
            sourceFile.deleteOnExit();
            String namespace = baseNamespace + Constants.point + Constants.map.get(protocol.getNamespace());

            String protoJson = protocol.toString(true);
            JSONObject jsonObject = JSONObject.parseObject(protoJson);
            // 调整协议类的包结构
            jsonObject.put(Constants.namespaceKey, namespace);

            setTypeNamespace(baseNamespace, protocol, jsonObject);

            FileUtils.writeJsonFile(sourceFile, jsonObject.toJSONString());
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
     * @param jsonObject    json
     */
    private void setTypeNamespace(String baseNamespace, Protocol protocol, JSONObject jsonObject) {
        String dtoNamespace;

        Collection<Schema> schemaColl = protocol.getTypes();
        Map<String, String> map = new HashMap<>(0);
        for (Schema schema : schemaColl) {
            if (protocol.getName().contains(Constants.querySuffix)) {
                // 查询api Controller
                dtoNamespace = baseNamespace + Constants.point
                        + String.format(
                        Constants.map.get("query" + (schema.getName().toLowerCase().endsWith("dto") ? "dto" : "vo")),
                        protocol.getName().replace(Constants.querySuffix, "").toLowerCase()
                );
            } else {
                // 应用api Controller
                dtoNamespace = baseNamespace + Constants.map.get((schema.getName().toLowerCase().endsWith("dto") ? "dto" : "vo"));
            }
            map.put(schema.getName(), dtoNamespace);
        }
        JSONArray typesJsonArray = jsonObject.getJSONArray(Constants.typesKey);
        for (int i = 0, len = typesJsonArray.size(); i < len; i++) {
            JSONObject typeJsonObject = typesJsonArray.getJSONObject(i);
            typeJsonObject.put(Constants.namespaceKey, map.get(typeJsonObject.getString(Constants.nameKey)));
        }
    }

    private String dealNamespace(File sourceFile, File srcDir) {
        String namespace;
        JSONObject projectConfigJson = loadJson(srcDir, Constants.projectJsonFileName);
        if (projectConfigJson.containsKey(Constants.packagePrefixKey)) {
            // 相对路径
            String relativePath = sourceFile.getParentFile().getAbsolutePath().replaceAll(
                    protocolDirectory.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\"), "");
            String[] pathArr = relativePath.substring(1).split("\\".equals(File.separator) ? "\\\\" : File.separator); // 模块，架构功能
            File moduleDir = new File(srcDir.getAbsolutePath(), pathArr[0]);
            JSONObject moduleConfigJson = loadJson(moduleDir, Constants.moduleJsonFileName);
            // 构造结构信息
            namespace = projectConfigJson.getString(Constants.packagePrefixKey);
            if (moduleConfigJson.containsKey(Constants.packagePrefixKey)) {
                namespace += Constants.point + moduleConfigJson.getString(Constants.packagePrefixKey);
            } else {
                namespace += Constants.point + pathArr[0];
            }
        } else {
            namespace = "";
        }
        return namespace;
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
