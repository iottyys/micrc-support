package io.ttyys.micrc.sad.gradle.plugin;

import io.ttyys.micrc.sad.gradle.plugin.common.Constants;
import io.ttyys.micrc.sad.gradle.plugin.task.GenerateAvroProtocolTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

public class SchemaDesignPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        project.getPlugins().apply(AvroBasePlugin.class);
        configureTasks(project);
    }

    private static void configureTasks(final Project project) {
        getSourceSets(project).configureEach(sourceSet -> {
            TaskProvider<GenerateAvroProtocolTask> protoTaskProvider = configureProtocolGenerationTask(project, sourceSet);
            configureTaskDependencies(project, sourceSet, protoTaskProvider);
        });
    }

    private static SourceSetContainer getSourceSets(Project project) {
        return project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
    }


    private static TaskProvider<GenerateAvroProtocolTask> configureProtocolGenerationTask(final Project project,
                                                                                          final SourceSet sourceSet) {
        String taskName = sourceSet.getTaskName("generate", "avroProtocol");
        return project.getTasks().register(taskName, GenerateAvroProtocolTask.class, task -> {
            task.setDescription(
                    String.format("Generates %s Avro protocol definition files from IDL files.", sourceSet.getName()));
            task.setGroup(Constants.GROUP_SOURCE_GENERATION);
            task.source(getAvroSourceDir(project, sourceSet));
            task.include("**/*." + Constants.IDL_EXTENSION);
            task.setClasspath(project.getConfigurations().getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME));
            task.getOutputDir().convention(getGeneratedOutputDir(project, sourceSet, Constants.PROTOCOL_EXTENSION));
        });
    }

    private static File getAvroSourceDir(Project project, SourceSet sourceSet) {
        return project.file(String.format("src/%s/avro", sourceSet.getName()));
    }

    private static Provider<Directory> getGeneratedOutputDir(Project project, SourceSet sourceSet, String extension) {
        String generatedOutputDirName = String.format("generated-%s-avro-%s", sourceSet.getName(), extension);
        return project.getLayout().getBuildDirectory().dir(generatedOutputDirName);
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
