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

import com.alibaba.fastjson.JSONObject;
import io.ttyys.micrc.sad.gradle.plugin.common.Constants;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileExtensionSpec;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FilenameUtils;
import org.apache.avro.Protocol;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.NotSpec;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.ttyys.micrc.sad.gradle.plugin.common.Constants.PROTOCOL_EXTENSION;

/**
 * 技术设计
 */
public class DealProtocolTechnologyTask extends OutputDirTask {

    private Map<String, JSONObject> moduleMap = new HashMap<>(0);

    private final Property<String> sourcePath;
    private final Property<String> destPath;

    @Inject
    public DealProtocolTechnologyTask(ObjectFactory objects) {
        super();
        this.sourcePath = objects.property(String.class).convention(Constants.PROTOCOL_SOURCE_PATH_KEY);
        this.destPath = objects.property(String.class).convention(Constants.PROTOCOL_DEST_PATH_KEY);
    }

    @TaskAction
    protected void process() {
        loadModuleJson();
        getLogger().info("Found {} files", getSource().getFiles().size());
        failOnUnsupportedFiles();
        processFiles();
    }

    private void loadModuleJson() {
        File destDir = new File(getProject().getProjectDir(), sourcePath.get());
        //noinspection ConstantConditions
        for (File file : destDir.listFiles()) {
            String jsonStr = FileUtils.readJsonString(file.getAbsolutePath() + File.separator + Constants.MODULE_JSON_FILE_NAME);
            JSONObject jsonObject = JSONObject.parseObject(jsonStr);
            moduleMap.put(file.getName(), jsonObject);
        }
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
            String requestType = sourceFile.getParentFile().getParentFile().getName();  // local rpc msg
            String activeState = sourceFile.getParentFile().getName(); // 主动 / 被动
            TechnologyType technologyType = TechnologyType.valueOf(requestType);
            Protocol protocol = Protocol.parse(sourceFile);
            sourceFile.deleteOnExit();
            // 添加注解  FIXME 没找到多注解的处理方案,暂时先用@拼接
            String annotation = protocol.getProp(Constants.JAVA_ANNOTATION_KEY);
            annotation = annotation == null ? "" : (annotation + '@');
            annotation += technologyType.getAnnotation(activeState,
                    FilenameUtils.getEndpointByProtocolFile(sourceFile),
                    FilenameUtils.removeExtension(sourceFile.getName()) + "Adapter");
            String protoJson = protocol.toString(true);
            JSONObject jsonObject = JSONObject.parseObject(protoJson);
            jsonObject.put(Constants.JAVA_ANNOTATION_KEY, annotation);
            FileUtils.writeJsonFile(sourceFile, Protocol.parse(jsonObject.toJSONString()).toString(true));
            getLogger().debug("协议添加结构注解完成 {}", sourceFile.getPath());
        } catch (IOException ex) {
            throw new GradleException(String.format("Failed to process protocol definition file %s", sourceFile), ex);
        }
    }

    private JSONObject getModuleJson(File sourceFile) {
        String destPathAbs = String.join(File.separator, getProject().getProjectDir().getAbsolutePath(), destPath.get(), "");
        String path = sourceFile.getAbsolutePath().replace(destPathAbs, "");
        path = path.substring(0, path.indexOf(File.separator));
        return moduleMap.get(path);
    }

    public Property<String> getSourcePath() {
        return sourcePath;
    }

    public Property<String> getDestPath() {
        return destPath;
    }

    public void setSourcePath(String schemaDesignPath) {
        this.sourcePath.set(schemaDesignPath);
    }

    public void setDestPath(String destPath) {
        this.destPath.set(destPath);
    }

    enum TechnologyType {
        local("LocalTransfer"),
        rpc("RpcTransfer"),
        msg("Information"),
        ;
        private String type;

        TechnologyType(String type) {
            this.type = type;
        }

        public String getAnnotation(String activeState, String endpoint, String implClassName) {
            if (Arrays.asList("called", "messaged").contains(activeState)) {
                return getConsumerAnnotation(endpoint, implClassName);
            } else {
                return getProducerAnnotation(endpoint, implClassName);
            }
        }

        private String getConsumerAnnotation(String endpoint, String implClassName) {
            String consumerAnnotationFmt = "io.ttyys.micrc.annotations.technology.%sConsumer(endpoint=\"%s\", adapterClassName=\"%s\")";
            return String.format(consumerAnnotationFmt, type, endpoint, implClassName);
        }

        private String getProducerAnnotation(String endpoint, String implClassName) {
            String producerAnnotationFmt = "io.ttyys.micrc.annotations.technology.%sProducer(endpoint=\"%s\", adapterClassName=\"%s\")";
            return String.format(producerAnnotationFmt, type, endpoint, implClassName);
        }
    }
}
