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

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import io.ttyys.micrc.sad.gradle.plugin.common.AvroUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.ProjectUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileExtensionSpec;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.gradle.GradleCompatibility;
import io.ttyys.micrc.sad.gradle.plugin.schema.Constants;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.NotSpec;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Task to convert Avro IDL files into Avro protocol files using {@link Idl}.
 */
@CacheableTask
public class CompileIdlTask extends OutputDirTask {

    private FileCollection classpath;

    private Project project;

    private File protocolDirectory;

    @Inject
    public CompileIdlTask() {
        project = getProject();
        classpath = GradleCompatibility.createConfigurableFileCollection(project);
        String protocolDirectoryPath = StringUtils.join(
                Arrays.asList(project.getBuildDir().getAbsolutePath(), Constants.protocolExtension),
                File.separator);
        protocolDirectory = new File(protocolDirectoryPath);
        if (!protocolDirectory.exists()) {
            protocolDirectory.mkdirs();
        }
    }

    @TaskAction
    protected void process() {
        getLogger().info("Found {} files", getSource().getFiles().size());
        failOnUnsupportedFiles();
        processFiles();
    }

    private void failOnUnsupportedFiles() {
        FileCollection unsupportedFiles = filterSources(new NotSpec<>(new FileExtensionSpec(Constants.idlExtension)));
        if (!unsupportedFiles.isEmpty()) {
            throw new GradleException(
                    String.format("Unsupported file extension for the following files: %s", unsupportedFiles));
        }
    }

    private void processFiles() {
        int processedFileCount = 0;
        SourceSet sourceSet = ProjectUtils.getMainSourceSet(project);
        File srcDir = ProjectUtils.getAvroSourceDir(project, sourceSet);
        for (File sourceFile : filterSources(new FileExtensionSpec(Constants.idlExtension))) {
            processIDLFile(sourceFile, srcDir);
            processedFileCount++;
        }
        setDidWork(processedFileCount > 0);
    }

    private void processIDLFile(File idlFile, File srcDir) {
        // 相对路径
        String relativePath = idlFile.getParentFile().getAbsolutePath().replaceAll(
                srcDir.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\"), "");
        String path = protocolDirectory.getAbsolutePath() + relativePath;
        String fileName = FileUtil.getPrefix(idlFile);
        getLogger().info("Processing {}", idlFile);
        try {
            Idl idl = new Idl(idlFile);
            Protocol protocol = idl.CompilationUnit();
            if (protocol.getMessages().isEmpty()) {
                // 类型定义 record(java类) enum(java枚举) -- 领域对象用到的多
                Iterator<Schema> it = protocol.getTypes().iterator();
                for (int i = 0; it.hasNext(); i++) {
                    Schema schema = it.next();
                    String schemaJson = schema.toString(true);
                    String currentName = String.format("%s%s%s%s%s",
                            fileName, Constants.schemaNumDelimiter, i + 1, Constants.point, Constants.schemaExtension);
                    File schemaFile = new File(path, currentName);
                    FileUtils.writeJsonFile(schemaFile, schemaJson);
                }
            } else {
                // 协议定义
                String protoJson = protocol.toString(true);
                File protoFile = new File(path, fileName + Constants.point + Constants.protocolExtension);
                FileUtils.writeJsonFile(protoFile, protoJson);
            }
            getLogger().debug("IDL（{}）编译完成", relativePath + idlFile.getName());
        } catch (IOException | ParseException ex) {
            throw new GradleException(String.format("Failed to compile IDL file %s", idlFile), ex);
        }
    }

    private void dealRelation(File idlFile, String protoJson, String srcDirPath, File srcDir) throws IOException {
        String jsonStr = FileUtils.readJsonString(idlFile.getParent() + File.separator + Constants.moduleJsonFileName);
        JSONObject jsonObject = JSONObject.parseObject(jsonStr);
        List<String> relationArray = jsonObject.getJSONArray(idlFile.getName().substring(0, idlFile.getName().length() - 1 - Constants.idlExtension.length())).toJavaList(String.class);
        Property<String> destPath = null;
        for (String moduleDir : relationArray) {
            String path = AvroUtils.assemblePath(idlFile, srcDirPath, false, moduleDir, destPath);
            File protoFile = new File(srcDir, path);
            FileUtils.writeJsonFile(protoFile, protoJson);
            getLogger().debug("写入协议 调用/监听 {}", protoFile.getPath());
        }
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    public void classpath(Object... paths) {
        this.classpath.plus(project.files(paths));
    }

    public File getProtocolDirectory() {
        return protocolDirectory;
    }

    @Classpath
    public FileCollection getClasspath() {
        return this.classpath;
    }
}
