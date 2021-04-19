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

import com.alibaba.fastjson.JSONArray;
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
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
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
import java.util.LinkedList;
import java.util.List;

import static io.ttyys.micrc.sad.gradle.plugin.common.Constants.IDL_EXTENSION;

/**
 * Task to convert Avro IDL files into Avro protocol files using {@link Idl}.
 */
@CacheableTask
public class GenerateAvroProtocolTask extends OutputDirTask {

    private FileCollection classpath;
    private final Property<String> schemaDesignPath;
    private final Property<String> serviceIntegrationPath;

    @Inject
    public GenerateAvroProtocolTask(ObjectFactory objects) {
        super();
        this.classpath = GradleCompatibility.createConfigurableFileCollection(getProject());
        this.schemaDesignPath = objects.property(String.class).convention(Constants.SCHEMA_DESIGN_PATH_KEY);
        this.serviceIntegrationPath = objects.property(String.class).convention(Constants.SERVICE_INTEGRATION_PATH_KEY);
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
        ClassLoader loader = assembleClassLoader();
        for (File sourceFile : filterSources(new FileExtensionSpec(IDL_EXTENSION))) {
            processIDLFile(sourceFile, loader);
            processedFileCount++;
        }
        setDidWork(processedFileCount > 0);
    }

    private void processIDLFile(File idlFile, ClassLoader loader) {
        SourceSet sourceSet = ProjectUtils.getMainSourceSet(getProject());
        String srcDirPath = ProjectUtils.getAvroSourceDirPath(sourceSet);
        File srcDir = ProjectUtils.getAvroSourceDir(getProject(), sourceSet);
        getLogger().info("Processing {}", idlFile);
        try (Idl idl = new Idl(idlFile, loader)) {
            Protocol protocol = idl.CompilationUnit();
            String path = AvroUtils.assemblePath(idlFile, srcDirPath, true, null, serviceIntegrationPath);
            File protoFile = new File(srcDir, path);
            String protoJson = protocol.toString(true);
            FileUtils.writeJsonFile(protoFile, protoJson);
            getLogger().debug("写入协议定义 {}", protoFile.getPath());

            String jsonStr = FileUtils.readJsonString(idlFile.getParent() + AvroUtils.UNIX_SEPARATOR + "relation.json");
            JSONObject jsonObject = JSONObject.parseObject(jsonStr);
            List<String> relationArray = jsonObject.getJSONArray(idlFile.getName().substring(0, idlFile.getName().length() - 1 - IDL_EXTENSION.length())).toJavaList(String.class);
            for (String moduleDir : relationArray) {
                path = AvroUtils.assemblePath(idlFile, srcDirPath, false, moduleDir, serviceIntegrationPath);
                protoFile = new File(srcDir, path);
                FileUtils.writeJsonFile(protoFile, protoJson);
                getLogger().debug("写入协议 调用/监听 {}", protoFile.getPath());
            }

        } catch (IOException | ParseException ex) {
            throw new GradleException(String.format("Failed to compile IDL file %s", idlFile), ex);
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

    public void setSchemaDesignPath(String schemaDesignPath) {
        this.schemaDesignPath.set(schemaDesignPath);
    }

    public void setServiceIntegrationPath(String serviceIntegrationPath) {
        this.serviceIntegrationPath.set(serviceIntegrationPath);
    }

    public Property<String> getSchemaDesignPath() {
        return schemaDesignPath;
    }

    public Property<String> getServiceIntegrationPath() {
        return serviceIntegrationPath;
    }
}
