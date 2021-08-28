package io.ttyys.micrc.sad.gradle.plugin.schema;

import io.ttyys.micrc.sad.gradle.plugin.common.Constants;
import io.ttyys.micrc.sad.gradle.plugin.common.ProjectUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.gradle.GradleCompatibility;
import io.ttyys.micrc.sad.gradle.plugin.extension.SchemaDesignExtension;
import io.ttyys.micrc.sad.gradle.plugin.schema.task.CompileIdlTask;
import io.ttyys.micrc.sad.gradle.plugin.schema.task.DealProtocolStructureTask;
import io.ttyys.micrc.sad.gradle.plugin.schema.task.DealProtocolTechnologyTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

public class SchemaDesignPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        configureTasks(project);
    }

    private static void configureTasks(Project project) {
        ProjectUtils.getSourceSets(project).configureEach(sourceSet -> {
            // src/main/avro 设计avro文件idl（后期替换为nacos），当前任务是将idl编译为protocol的json文件，编译结果目录 build/avpr
            TaskProvider<CompileIdlTask> compileIdlTaskProvider = configureCompileIdlTask(project, sourceSet);
            // 根据驱动关系，由消息生产者（方法定义者）先定义协议，然后根据消费（调用）关系，copy到消费者（调用者）方
            // 在目录build/avpr下处理所有protocol的json文件中的包结构（namespace）
            TaskProvider<DealProtocolStructureTask> structureDesignTask = configureStructureDesignTask(project, sourceSet, compileIdlTaskProvider);
            // 在目录build/avpr下处理所有protocol的json文件中的技术注解（javaAnnotation）
            TaskProvider<DealProtocolTechnologyTask> technologyDesignTask = configureTechnologyDesignTask(project, sourceSet, structureDesignTask);
            ProjectUtils.configureTaskDependencies(project, sourceSet, technologyDesignTask);
        });
    }

    private static TaskProvider<CompileIdlTask> configureCompileIdlTask(Project project,
                                                                        SourceSet sourceSet) {
        String taskName = sourceSet.getTaskName("generate", "avroProtocol");
        return project.getTasks().register(taskName, CompileIdlTask.class, task -> {
            task.setDescription(
                    String.format("Generates %s Avro protocol definition files from IDL files.", sourceSet.getName()));
            task.source(ProjectUtils.getAvroSourceDir(project, sourceSet));
            task.include("**/*." + Constants.IDL_EXTENSION);
            task.setClasspath(project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME));
            task.getOutputDir().convention(ProjectUtils.getGeneratedOutputDir(project, sourceSet, Constants.PROTOCOL_EXTENSION));
        });
    }

    private static TaskProvider<DealProtocolStructureTask> configureStructureDesignTask(Project project,
                                                                                        SourceSet sourceSet,
                                                                                        TaskProvider<CompileIdlTask> protoTaskProvider) {
        String taskName = sourceSet.getTaskName("design", "Structure");
        return project.getTasks().register(taskName, DealProtocolStructureTask.class, task -> {
            task.setDescription(
                    String.format("Design structure %s Avro protocol definition files from self.", sourceSet.getName()));
            task.source(protoTaskProvider);
            File protocolDirectory = protoTaskProvider.get().getProtocolDirectory();
            task.source(protocolDirectory);
            task.setProtocolDirectory(protocolDirectory);
            task.include("**/*." + Constants.PROTOCOL_EXTENSION);
            task.getOutputDir().convention(ProjectUtils.getGeneratedOutputDir(project, sourceSet, Constants.PROTOCOL_EXTENSION));
        });
    }

    private static TaskProvider<DealProtocolTechnologyTask> configureTechnologyDesignTask(Project project,
                                                                                          SourceSet sourceSet,
                                                                                          TaskProvider<DealProtocolStructureTask> protoTaskProvider) {
        String taskName = sourceSet.getTaskName("design", "Technology");
        return project.getTasks().register(taskName, DealProtocolTechnologyTask.class, task -> {
            task.setDescription(
                    String.format("Design technology %s Avro protocol definition files from self.", sourceSet.getName()));
            task.source(protoTaskProvider);
            task.source(ProjectUtils.getAvroSourceDir(project, sourceSet));
            task.include("**/*." + Constants.PROTOCOL_EXTENSION);
            task.getOutputDir().convention(ProjectUtils.getGeneratedOutputDir(project, sourceSet, Constants.PROTOCOL_EXTENSION));
        });
    }
}
