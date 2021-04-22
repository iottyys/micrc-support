package io.ttyys.micrc.sad.gradle.plugin;

import io.ttyys.micrc.sad.gradle.plugin.common.Constants;
import io.ttyys.micrc.sad.gradle.plugin.common.ProjectUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.gradle.GradleCompatibility;
import io.ttyys.micrc.sad.gradle.plugin.extension.SchemaDesignExtension;
import io.ttyys.micrc.sad.gradle.plugin.task.ClearAvroProtocolTask;
import io.ttyys.micrc.sad.gradle.plugin.task.GenerateAvroProtocolTask;
import io.ttyys.micrc.sad.gradle.plugin.task.StructureDesignAvroProtocolTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

public class SchemaDesignPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        SchemaDesignExtension schemaDesignExtension = configureExtension(project);
        configureTasks(project, schemaDesignExtension);
    }

    private static SchemaDesignExtension configureExtension(final Project project) {
        final SchemaDesignExtension schemaDesignExtension =
                GradleCompatibility.createExtensionWithObjectFactory(project, Constants.SCHEMA_DESIGN_EXTENSION_NAME, SchemaDesignExtension.class);
        project.getTasks().withType(GenerateAvroProtocolTask.class).configureEach(task -> {
            task.getDestPath().convention(schemaDesignExtension.getDestPath());
        });
        return schemaDesignExtension;
    }

    private static void configureTasks(final Project project, SchemaDesignExtension extension) {
        TaskContainer taskContainer = project.getTasks();
        ProjectUtils.getSourceSets(project).configureEach(sourceSet -> {
            TaskProvider<ClearAvroProtocolTask> clearTaskProvider = configureClearAvroProtocolTask(taskContainer, sourceSet, extension);
            TaskProvider<GenerateAvroProtocolTask> protoTaskProvider = configureProtocolGenerationTask(project, sourceSet, clearTaskProvider);
            TaskProvider<StructureDesignAvroProtocolTask> structureDesignTask = configureStructureDesignTask(project, sourceSet, protoTaskProvider);
            configureTaskDependencies(project, sourceSet, structureDesignTask);
        });
    }

    private static TaskProvider<ClearAvroProtocolTask> configureClearAvroProtocolTask(TaskContainer taskContainer,
                                                                                      SourceSet sourceSet,
                                                                                      SchemaDesignExtension extension) {
        String taskName = sourceSet.getTaskName("clear", "avroProtocol");
        return taskContainer.register(taskName, ClearAvroProtocolTask.class, task -> {
            task.setDestPath(ProjectUtils.getAvroSourceDirPath(sourceSet) + File.separator + extension.getDestPath().get());
        });
    }

    private static TaskProvider<GenerateAvroProtocolTask> configureProtocolGenerationTask(final Project project,
                                                                                          final SourceSet sourceSet,
                                                                                          TaskProvider<ClearAvroProtocolTask> clearTaskProvider) {
        String taskName = sourceSet.getTaskName("generate", "avroProtocol");
        return project.getTasks().register(taskName, GenerateAvroProtocolTask.class, task -> {
            task.setDescription(
                    String.format("Generates %s Avro protocol definition files from IDL files.", sourceSet.getName()));
            task.source(ProjectUtils.getAvroSourceDir(project, sourceSet));
            task.source(clearTaskProvider);
            task.include("**/*." + Constants.IDL_EXTENSION);
            task.setClasspath(project.getConfigurations().getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME));
            task.getOutputDir().convention(ProjectUtils.getGeneratedOutputDir(project, sourceSet, Constants.PROTOCOL_EXTENSION));
        });
    }

    private static TaskProvider<StructureDesignAvroProtocolTask> configureStructureDesignTask(final Project project,
                                                                                              final SourceSet sourceSet,
                                                                                              TaskProvider<GenerateAvroProtocolTask> protoTaskProvider) {
        String taskName = sourceSet.getTaskName("design", "Structure");
        return project.getTasks().register(taskName, StructureDesignAvroProtocolTask.class, task -> {
            task.setDescription(
                    String.format("Design structure %s Avro protocol definition files from self.", sourceSet.getName()));
            task.source(ProjectUtils.getAvroSourceDir(project, sourceSet));
            task.source(protoTaskProvider);
            task.include("**/*." + Constants.PROTOCOL_EXTENSION);
            task.getOutputDir().convention(ProjectUtils.getGeneratedOutputDir(project, sourceSet, Constants.PROTOCOL_EXTENSION));
        });
    }

    private static void configureTaskDependencies(final Project project, final SourceSet sourceSet,
                                                  final TaskProvider<StructureDesignAvroProtocolTask> javaTaskProvider) {
        project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", appliedPlugin ->
                project.getTasks()
                        .withType(SourceTask.class)
                        .matching(task -> sourceSet.getCompileTaskName("kotlin").equals(task.getName()))
                        .configureEach(task -> task.source(javaTaskProvider.get().getOutputs()))
        );
    }
}
