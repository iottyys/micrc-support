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
package io.ttyys.micrc.sad.gradle.plugin.task;

import io.ttyys.micrc.sad.gradle.plugin.common.Constants;
import io.ttyys.micrc.sad.gradle.plugin.common.RequestEnum;
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
import java.util.Arrays;

import static io.ttyys.micrc.sad.gradle.plugin.common.Constants.PROTOCOL_EXTENSION;

/**
 * Task to convert Avro IDL files into Avro protocol files using {@link Idl}.
 */
public class StructureDesignTask extends OutputDirTask {
    private static final String Consumer_Annotation_Str = "io.ttyys.micrc.annotations.Structure(interfacePkg=\"infrastructure.%s\", implPkg=\"infrastructure.%s\", objPkg=\"infrastructure.dto\")";
    private static final String Producer_Annotation_Str = "io.ttyys.micrc.annotations.Structure(interfacePkg=\"domain.service.%s\", implPkg=\"infrastructure.%s\", objPkg=\"domain.service.vo\")";
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
//            String requestType = sourceFile.getParentFile().getParentFile().getName();  // local rpc msg
            String activeState = sourceFile.getParentFile().getName(); // 主动 / 被动
            String annotation;
            if (Arrays.asList(RequestEnum.LOCAL.getDefDirName(), RequestEnum.INFORMATION.getDefDirName()).contains(activeState)) {
                annotation = String.format(Consumer_Annotation_Str, activeState, activeState);
            } else {
                annotation = String.format(Producer_Annotation_Str, activeState, activeState);
            }
            sourceFile.deleteOnExit();
            // 添加结构性注解
            protocol.addProp("javaAnnotation", annotation);
            String protoJson = protocol.toString(true);
            FileUtils.writeJsonFile(sourceFile, protoJson);
            getLogger().debug("协议添加结构注解完成 {}", sourceFile.getPath());
        } catch (IOException ex) {
            throw new GradleException(String.format("Failed to process protocol definition file %s", sourceFile), ex);
        }
    }

    public String getGroup() {
        return Constants.GROUP_SOURCE_GENERATION;
    }
}
