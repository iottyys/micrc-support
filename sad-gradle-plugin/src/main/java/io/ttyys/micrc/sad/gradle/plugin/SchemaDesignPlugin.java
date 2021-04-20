package io.ttyys.micrc.sad.gradle.plugin;

import io.ttyys.micrc.sad.gradle.plugin.common.Constants;
import io.ttyys.micrc.sad.gradle.plugin.common.ProjectUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.gradle.GradleCompatibility;
import io.ttyys.micrc.sad.gradle.plugin.extension.SchemaDesignExtension;
import io.ttyys.micrc.sad.gradle.plugin.task.GenerateAvroProtocolTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskProvider;

import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

public class SchemaDesignPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        configureExtension(project);
        configureTasks(project);
    }

    private static void configureExtension(final Project project) {
        final SchemaDesignExtension schemaDesignExtension =
                GradleCompatibility.createExtensionWithObjectFactory(project, Constants.SCHEMA_DESIGN_EXTENSION_NAME, SchemaDesignExtension.class);
        project.getTasks().withType(GenerateAvroProtocolTask.class).configureEach(task -> {
            task.getSchemaDesignPath().convention(schemaDesignExtension.getSchemaDesignPath());
            task.getServiceIntegrationPath().convention(schemaDesignExtension.getServiceIntegrationPath());
        });
    }

    private static void configureTasks(final Project project) {
        ProjectUtils.getSourceSets(project).configureEach(sourceSet -> {
            TaskProvider<GenerateAvroProtocolTask> protoTaskProvider = configureProtocolGenerationTask(project, sourceSet);
            configureTaskDependencies(project, sourceSet, protoTaskProvider);
        });
    }

    private static TaskProvider<GenerateAvroProtocolTask> configureProtocolGenerationTask(final Project project,
                                                                                          final SourceSet sourceSet) {
        String taskName = sourceSet.getTaskName("generate", "avroProtocol");
        return project.getTasks().register(taskName, GenerateAvroProtocolTask.class, task -> {
            task.setDescription(
                    String.format("Generates %s Avro protocol definition files from IDL files.", sourceSet.getName()));
            task.setGroup(Constants.GROUP_SOURCE_GENERATION);
            task.source(ProjectUtils.getAvroSourceDir(project, sourceSet));
            task.include("**/*." + Constants.IDL_EXTENSION);
            task.setClasspath(project.getConfigurations().getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME));
            task.getOutputDir().convention(ProjectUtils.getGeneratedOutputDir(project, sourceSet, Constants.PROTOCOL_EXTENSION));
        });
    }

    private static void configureTaskDependencies(final Project project, final SourceSet sourceSet,
                                                  final TaskProvider<GenerateAvroProtocolTask> javaTaskProvider) {
        project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", appliedPlugin ->
                project.getTasks()
                        .withType(SourceTask.class)
                        .matching(task -> sourceSet.getCompileTaskName("kotlin").equals(task.getName()))
                        .configureEach(task -> task.source(javaTaskProvider.get().getOutputs()))
        );
    }
}
