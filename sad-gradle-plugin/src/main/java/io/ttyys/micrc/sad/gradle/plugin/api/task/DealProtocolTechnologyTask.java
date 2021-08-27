package io.ttyys.micrc.sad.gradle.plugin.api.task;

import com.alibaba.fastjson.JSONObject;
import io.ttyys.micrc.sad.gradle.plugin.api.Constants;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileExtensionSpec;
import io.ttyys.micrc.sad.gradle.plugin.common.file.FileUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.gradle.GradleCompatibility;
import io.ttyys.micrc.sad.gradle.plugin.schema.task.OutputDirTask;
import org.apache.avro.Protocol;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.NotSpec;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

import static io.ttyys.micrc.sad.gradle.plugin.api.Constants.*;

@CacheableTask
public class DealProtocolTechnologyTask extends OutputDirTask {

    @Classpath
    private FileCollection classpath;

    private Project project;

    @Inject
    public DealProtocolTechnologyTask() {
        classpath = GradleCompatibility.createConfigurableFileCollection(getProject());
        project = getProject();
    }

    @TaskAction
    public void process() {
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
            String annotation = protocol.getProp(JAVA_ANNOTATION_KEY);
            // 构造技术性注解信息 FIXME 没找到多注解的处理方案,暂时先用@拼接
            annotation += "";
            String protoJson = protocol.toString(true);
            JSONObject jsonObject = JSONObject.parseObject(protoJson);
            // 添加技术性注解
            jsonObject.put(JAVA_ANNOTATION_KEY, annotation);
            FileUtils.writeJsonFile(sourceFile, Protocol.parse(jsonObject.toJSONString()).toString(true));
            getLogger().debug("协议添加结构注解完成 {}", sourceFile.getPath());
        } catch (IOException e) {
            throw new GradleException(String.format("Failed to process protocol definition file %s", sourceFile), e);
        }
    }

    public DealProtocolTechnologyTask setClasspath(FileCollection classpath) {
        this.classpath = classpath;
        return this;
    }

    @Classpath
    public FileCollection getClasspath() {
        return this.classpath;
    }

    @Override
    public String getGroup() {
        return Constants.GROUP;
    }
}
