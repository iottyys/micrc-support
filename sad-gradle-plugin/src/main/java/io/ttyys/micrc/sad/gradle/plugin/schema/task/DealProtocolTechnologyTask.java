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
import io.ttyys.micrc.sad.gradle.plugin.common.ProjectUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileExtensionSpec;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileUtils;
import io.ttyys.micrc.sad.gradle.plugin.schema.Constants;
import org.apache.avro.Protocol;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.specs.NotSpec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;


/**
 * 技术设计
 */
public class DealProtocolTechnologyTask extends OutputDirTask {

    private final Project project;
    private File protocolDirectory;

    @Inject
    public DealProtocolTechnologyTask(ObjectFactory objects) {
        super();
        project = getProject();
    }

    @TaskAction
    protected void process() {
        getLogger().info("Found {} files", getSource().getFiles().size());
        failOnUnsupportedFiles();
        processFiles();
    }

    private void failOnUnsupportedFiles() {
        FileCollection unsupportedFiles = filterSources(new NotSpec<>(new FileExtensionSpec(Constants.protocolExtension)));
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

        for (File sourceFile : filterSources(new FileExtensionSpec(Constants.protocolExtension))) {
            // 调整完成协议的包结构之后，通过上面保存的map将所有存在引用的类型应用处也进行调整
            String modulePkg = getModulePkg(projectPkg, sourceFile, srcDir);
            processProtocolFile(sourceFile, modulePkg);
            processedFileCount++;
        }
        setDidWork(processedFileCount > 0);
    }

    private void processProtocolFile(File sourceFile, String modulePkg) {
        try {
            Protocol protocol = Protocol.parse(sourceFile);
            sourceFile.deleteOnExit();
            // 添加注解  FIXME 没找到多注解的处理方案,暂时先用@拼接
            String annotation = protocol.getProp(Constants.javaAnnotationKey);
            annotation = annotation == null ? "" : (annotation + '@');
            String protoJson = protocol.toString(true);
            JSONObject jsonObject = JSONObject.parseObject(protoJson);
            jsonObject.put(Constants.javaAnnotationKey, annotation);
            FileUtils.writeJsonFile(sourceFile, Protocol.parse(jsonObject.toJSONString()).toString(true));
            getLogger().debug("协议添加结构注解完成 {}", sourceFile.getPath());
        } catch (IOException ex) {
            throw new GradleException(String.format("Failed to process protocol definition file %s", sourceFile), ex);
        }
    }

    private String getProjectPackage(File srcDir) {
        String namespace;
        JSONObject projectConfigJson = loadJson(srcDir, io.ttyys.micrc.sad.gradle.plugin.schema.Constants.projectJsonFileName);
        if (projectConfigJson.containsKey(io.ttyys.micrc.sad.gradle.plugin.schema.Constants.packagePrefixKey)) {
            namespace = projectConfigJson.getString(io.ttyys.micrc.sad.gradle.plugin.schema.Constants.packagePrefixKey);
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
        JSONObject moduleConfigJson = loadJson(moduleDir, io.ttyys.micrc.sad.gradle.plugin.schema.Constants.moduleJsonFileName);
        // 构造结构信息
        return projectPkg + io.ttyys.micrc.sad.gradle.plugin.schema.Constants.point + moduleConfigJson.getOrDefault(io.ttyys.micrc.sad.gradle.plugin.schema.Constants.packagePrefixKey, pathArr[0]);
    }

    public JSONObject loadJson(File srcDir, String fileName) {
        String jsonStr = FileUtils.readJsonString(srcDir.getAbsolutePath() + File.separator + fileName);
        return JSONObject.parseObject(jsonStr);
    }

    public DealProtocolTechnologyTask setProtocolDirectory(File protocolDirectory) {
        this.protocolDirectory = protocolDirectory;
        return this;
    }
}
