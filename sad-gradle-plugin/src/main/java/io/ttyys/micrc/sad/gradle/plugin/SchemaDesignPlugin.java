package io.ttyys.micrc.sad.gradle.plugin;

import io.ttyys.micrc.sad.gradle.plugin.common.Constants;
import io.ttyys.micrc.sad.gradle.plugin.common.ProjectUtils;
import io.ttyys.micrc.sad.gradle.plugin.common.SetBuilder;
import io.ttyys.micrc.sad.gradle.plugin.common.gradle.GradleCompatibility;
import io.ttyys.micrc.sad.gradle.plugin.extension.AvroExtension;
import io.ttyys.micrc.sad.gradle.plugin.extension.DefaultAvroExtension;
import io.ttyys.micrc.sad.gradle.plugin.extension.SchemaDesignExtension;
import io.ttyys.micrc.sad.gradle.plugin.task.GenerateAvroJavaTask;
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
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.idea.GenerateIdeaModule;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.Optional;

import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

public class SchemaDesignPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        project.getPlugins().apply(AvroBasePlugin.class);
        configureExtension(project);
        configureTasks(project);
        configureIntelliJ(project);
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
            TaskProvider<GenerateAvroJavaTask> javaTaskProvider = configureJavaGenerationTask(project, sourceSet, protoTaskProvider);
            configureTaskDependencies(project, sourceSet, javaTaskProvider);
        });
    }

    private static void configureIntelliJ(final Project project) {
        project.getPlugins().withType(IdeaPlugin.class).configureEach(ideaPlugin -> {
            SourceSet mainSourceSet = ProjectUtils.getMainSourceSet(project);
            SourceSet testSourceSet = ProjectUtils.getTestSourceSet(project);
            IdeaModule module = ideaPlugin.getModel().getModule();
            module.setSourceDirs(new SetBuilder<File>()
                    .addAll(module.getSourceDirs())
                    .add(ProjectUtils.getAvroSourceDir(project, mainSourceSet))
                    .add(ProjectUtils.getGeneratedOutputDir(project, mainSourceSet, Constants.JAVA_EXTENSION).map(Directory::getAsFile).get())
                    .build());
            module.setTestSourceDirs(new SetBuilder<File>()
                    .addAll(module.getTestSourceDirs())
                    .add(ProjectUtils.getAvroSourceDir(project, testSourceSet))
                    .add(ProjectUtils.getGeneratedOutputDir(project, testSourceSet, Constants.JAVA_EXTENSION).map(Directory::getAsFile).get())
                    .build());
            // IntelliJ doesn't allow source directories beneath an excluded directory.
            // Thus, we remove the build directory exclude and add all non-generated sub-directories as excludes.
            SetBuilder<File> excludeDirs = new SetBuilder<>();
            excludeDirs.addAll(module.getExcludeDirs()).remove(project.getBuildDir());
            File buildDir = project.getBuildDir();
            if (buildDir.isDirectory()) {
                excludeDirs.addAll(project.getBuildDir().listFiles(new NonGeneratedDirectoryFileFilter()));
            }
            module.setExcludeDirs(excludeDirs.build());
        });
        project.getTasks().withType(GenerateIdeaModule.class).configureEach(generateIdeaModule ->
                generateIdeaModule.doFirst(task ->
                        project.getTasks().withType(GenerateAvroJavaTask.class, generateAvroJavaTask ->
                                project.mkdir(generateAvroJavaTask.getOutputDir().get()))));
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

    private static TaskProvider<GenerateAvroJavaTask> configureJavaGenerationTask(final Project project, final SourceSet sourceSet,
                                                                                  TaskProvider<GenerateAvroProtocolTask> protoTaskProvider) {
        String taskName = sourceSet.getTaskName("generate", "avroJava");
        TaskProvider<GenerateAvroJavaTask> javaTaskProvider = project.getTasks().register(taskName, GenerateAvroJavaTask.class, task -> {
            task.setDescription(String.format("Generates %s Avro Java source files from schema/protocol definition files.",
                    sourceSet.getName()));
            task.setGroup(Constants.GROUP_SOURCE_GENERATION);
            task.source(ProjectUtils.getAvroSourceDir(project, sourceSet));
            task.source(protoTaskProvider);
            task.include("**/*." + Constants.SCHEMA_EXTENSION, "**/*." + Constants.PROTOCOL_EXTENSION);
            task.getOutputDir().convention(ProjectUtils.getGeneratedOutputDir(project, sourceSet, Constants.JAVA_EXTENSION));

            sourceSet.getJava().srcDir(task.getOutputDir());

            JavaCompile compileJavaTask = project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class).get();
            task.getOutputCharacterEncoding().convention(project.provider(() ->
                    Optional.ofNullable(compileJavaTask.getOptions().getEncoding()).orElse(Charset.defaultCharset().name())));
        });
        project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class, compileJavaTask -> {
            compileJavaTask.source(javaTaskProvider);
        });
        return javaTaskProvider;
    }

    private static void configureTaskDependencies(final Project project, final SourceSet sourceSet,
                                                  final TaskProvider<GenerateAvroJavaTask> javaTaskProvider) {
        project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", appliedPlugin ->
                project.getTasks()
                        .withType(SourceTask.class)
                        .matching(task -> sourceSet.getCompileTaskName("kotlin").equals(task.getName()))
                        .configureEach(task -> task.source(javaTaskProvider.get().getOutputs()))
        );
    }


    private static class NonGeneratedDirectoryFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory() && !file.getName().startsWith("generated-");
        }
    }
}
