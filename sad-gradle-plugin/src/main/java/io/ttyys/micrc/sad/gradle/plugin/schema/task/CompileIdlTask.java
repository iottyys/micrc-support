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
import io.ttyys.micrc.sad.gradle.plugin.common.Constants;
import io.ttyys.micrc.sad.gradle.plugin.common.ProjectUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileExtensionSpec;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.gradle.GradleCompatibility;
import org.apache.avro.Protocol;
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
import java.util.LinkedList;
import java.util.List;

import static io.ttyys.micrc.sad.gradle.plugin.api.Constants.POINT;
import static io.ttyys.micrc.sad.gradle.plugin.api.Constants.PROTOCOL_EXTENSION;
import static io.ttyys.micrc.sad.gradle.plugin.common.Constants.IDL_EXTENSION;

/**
 * Task to convert Avro IDL files into Avro protocol files using {@link Idl}.
 */
@CacheableTask
public class CompileIdlTask extends OutputDirTask {

    private FileCollection classpath;

    private Project project;

    @Inject
    public CompileIdlTask() {
        project = getProject();
        classpath = GradleCompatibility.createConfigurableFileCollection(project);
    }

    @TaskAction
    protected void process() {
        getLogger().info("Found {} files", getSource().getFiles().size());
        failOnUnsupportedFiles();
        processFiles();
    }

    private void failOnUnsupportedFiles() {
        FileCollection unsupportedFiles = filterSources(new NotSpec<>(new FileExtensionSpec(IDL_EXTENSION)));
        if (!unsupportedFiles.isEmpty()) {
            throw new GradleException(
                    String.format("Unsupported file extension for the following files: %s", unsupportedFiles));
        }
    }

    private void processFiles() {
        int processedFileCount = 0;
        SourceSet sourceSet = ProjectUtils.getMainSourceSet(getProject());
        File srcDir = ProjectUtils.getAvroSourceDir(getProject(), sourceSet);
        ClassLoader loader = assembleClassLoader();
        for (File sourceFile : filterSources(new FileExtensionSpec(IDL_EXTENSION))) {
            processIDLFile(sourceFile, loader, srcDir);
            processedFileCount++;
        }
        setDidWork(processedFileCount > 0);
    }

    private void processIDLFile(File idlFile, ClassLoader loader, File srcDir) {
        // 相对路径
        String relativePath = idlFile.getParentFile().getAbsolutePath().replaceAll(srcDir.getAbsolutePath(), "");
        String path = StringUtils.join(
                Arrays.asList(project.getBuildDir().getAbsolutePath(), PROTOCOL_EXTENSION, relativePath),
                File.separator);
        String protoFileName = FileUtil.getPrefix(idlFile) + POINT + PROTOCOL_EXTENSION;
        getLogger().info("Processing {}", idlFile);
        try (Idl idl = new Idl(idlFile, loader)) {
            Protocol protocol = idl.CompilationUnit();
            File protoFile = new File(path, protoFileName);
            String protoJson = protocol.toString(true);
            FileUtils.writeJsonFile(protoFile, protoJson);
            getLogger().debug("写入协议定义 {}", protoFile.getPath());
        } catch (IOException | ParseException ex) {
            throw new GradleException(String.format("Failed to compile IDL file %s", idlFile), ex);
        }
    }

    private void dealRelation(File idlFile, String protoJson, String srcDirPath, File srcDir) throws IOException {
        String jsonStr = FileUtils.readJsonString(idlFile.getParent() + File.separator + Constants.RELATION_JSON_FILE_NAME);
        JSONObject jsonObject = JSONObject.parseObject(jsonStr);
        List<String> relationArray = jsonObject.getJSONArray(idlFile.getName().substring(0, idlFile.getName().length() - 1 - IDL_EXTENSION.length())).toJavaList(String.class);
        Property<String> destPath = null;
        for (String moduleDir : relationArray) {
            String path = AvroUtils.assemblePath(idlFile, srcDirPath, false, moduleDir, destPath);
            File protoFile = new File(srcDir, path);
            FileUtils.writeJsonFile(protoFile, protoJson);
            getLogger().debug("写入协议 调用/监听 {}", protoFile.getPath());
        }
    }

    private ClassLoader assembleClassLoader() {
        List<URL> urls = new LinkedList<>();
        for (File file : classpath) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                getLogger().debug(e.getMessage());
            }
        }
        if (urls.isEmpty()) {
            getLogger().debug("No classpath configured; defaulting to system classloader");
        }
        return urls.isEmpty() ? ClassLoader.getSystemClassLoader()
                : new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    public void classpath(Object... paths) {
        this.classpath.plus(getProject().files(paths));
    }

    @Classpath
    public FileCollection getClasspath() {
        return this.classpath;
    }
}
