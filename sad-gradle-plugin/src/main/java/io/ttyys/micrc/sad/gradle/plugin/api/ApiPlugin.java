package io.ttyys.micrc.sad.gradle.plugin.api;

import io.ttyys.micrc.sad.gradle.plugin.api.task.CleanTask;
import io.ttyys.micrc.sad.gradle.plugin.api.task.CompileIdlTask;
import io.ttyys.micrc.sad.gradle.plugin.common.Constants;
import io.ttyys.micrc.sad.gradle.plugin.common.ProjectUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

public class ApiPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        ProjectUtils.getSourceSets(project).configureEach(sourceSet -> {
            TaskProvider<CleanTask> cleanTaskProvider = configureCleanTask(project, sourceSet);
            TaskProvider<CompileIdlTask> compileIdlTaskProvider = configureCompileIdlTask(project, sourceSet, cleanTaskProvider);
            ProjectUtils.configureTaskDependencies(project, sourceSet, compileIdlTaskProvider);
        });
    }

    private TaskProvider<CleanTask> configureCleanTask(Project project, SourceSet sourceSet) {
        String taskName = sourceSet.getTaskName("clear", "api all");
        return project.getTasks().register(taskName, CleanTask.class, task -> {
            task.source(ProjectUtils.getAvroSourceDir(project, sourceSet));
        });
    }

    private TaskProvider<CompileIdlTask> configureCompileIdlTask(Project project, SourceSet sourceSet, TaskProvider<CleanTask> cleanTaskProvider) {
        String taskName = sourceSet.getTaskName("compileIdl2Protocol", "api all");
        return project.getTasks().register(taskName, CompileIdlTask.class, task -> {
            task.source(ProjectUtils.getAvroSourceDir(project, sourceSet));
            task.source(cleanTaskProvider);
            task.include("**/*." + Constants.IDL_EXTENSION);
            task.setClasspath(project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME));
            task.getOutputDir().convention(ProjectUtils.getGeneratedOutputDir(project, sourceSet, Constants.PROTOCOL_EXTENSION));
        });
    }
}
