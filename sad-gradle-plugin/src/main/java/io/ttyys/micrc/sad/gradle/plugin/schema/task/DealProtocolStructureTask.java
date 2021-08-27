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

import io.ttyys.micrc.sad.gradle.plugin.common.file.FileExtensionSpec;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileUtils;
import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.Idl;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.NotSpec;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

import static io.ttyys.micrc.sad.gradle.plugin.common.Constants.PROTOCOL_EXTENSION;

/**
 * 设计结构说明 Structure
 * 设计源 src/main/avro
 *  ├- project-config.json -- 项目相关的配置（项目前缀包名/部署方式/各部分的技术定义/等）
 *  └- 模块目录
 *    ├- module-config.json -- 模块相关配置（模块前缀包名/本地消息调用关系/等）
 *    ├- api(controller)
 *    ├- application(service应用/查询服务)
 *    ├- domain
 *    ├- local
 */
public class DealProtocolStructureTask extends OutputDirTask {
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
        for (File sourceFile : filterSources(new FileExtensionSpec(PROTOCOL_EXTENSION))) {
            processProtocolFile(sourceFile);
            processedFileCount++;
        }
        setDidWork(processedFileCount > 0);
    }

    private void processProtocolFile(File sourceFile) {
        try {
            Protocol protocol = Protocol.parse(sourceFile);
            sourceFile.deleteOnExit();
            // 构造结构性注解信息
            // 添加结构性注解
            protocol.addProp("javaAnnotation", "annotation");
            String protoJson = protocol.toString(true);
            FileUtils.writeJsonFile(sourceFile, protoJson);
            getLogger().debug("协议调整结构完成 {}", sourceFile.getPath());
        } catch (IOException ex) {
            throw new GradleException(String.format("Failed to process protocol definition file %s", sourceFile), ex);
        }
    }
}
