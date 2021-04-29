package io.ttyys.micrc.codegen.gradle.plugin;

import io.ttyys.micrc.codegen.gradle.plugin.common.Constants;
import io.ttyys.micrc.codegen.gradle.plugin.common.ProjectUtils;
import io.ttyys.micrc.codegen.gradle.plugin.common.SetBuilder;
import io.ttyys.micrc.codegen.gradle.plugin.common.gradle.GradleCompatibility;
import io.ttyys.micrc.codegen.gradle.plugin.extension.CodeGeneratorExtension;
import io.ttyys.micrc.codegen.gradle.plugin.task.ClearAvroJavaTask;
import io.ttyys.micrc.codegen.gradle.plugin.task.GenerateAvroJavaTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.idea.GenerateIdeaModule;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Set;


public class CodeGeneratorPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        /* buildScript 块
        ScriptHandler scriptHandler = project.getBuildscript();
        DependencyHandler scriptDependencyHandler = scriptHandler.getDependencies();
        scriptDependencyHandler.add("classpath", scriptDependencyHandler.create("io.ttyys.micrc:codegen-gradle-plugin:0.0.1-SNAPSHOT"));

        PluginContainer pluginContainer = project.getPlugins();
        pluginContainer.apply("io.ttyys.gradle.codegen.codegen-gradle-plugin");
        pluginContainer.apply("io.ttyys.gradle.codegen.integration-gradle-plugin");*/

        DependencyHandler dependencyHandler = project.getDependencies();
        dependencyHandler.add("implementation", dependencyHandler.create("org.apache.avro:avro-compiler:1.10.2"));
        dependencyHandler.add("implementation", dependencyHandler.create("org.apache.avro:avro-ipc:1.10.2"));
        dependencyHandler.add("compile", dependencyHandler.create("io.ttyys.micrc:micrc-annotations:0.0.1-SNAPSHOT"));
        dependencyHandler.add("annotationProcessor", dependencyHandler.create("io.ttyys.micrc:codegen-gradle-plugin:0.0.1-SNAPSHOT"));
        // 通用配置
        // 注解处理器配置
        // 引入其它插件，客户端只引入这个插件，可以具备所有能力
        TaskContainer taskContainer = project.getTasks();
        ClearAvroJavaTask clearAvroJavaTask = taskContainer.create("clearAvroJavaTask", ClearAvroJavaTask.class);
        taskContainer.add(clearAvroJavaTask);
        configureExtension(project);
        configureTasks(project, clearAvroJavaTask);
        configureIntelliJ(project);
    }

    private static CodeGeneratorExtension configureExtension(final Project project) {
        final CodeGeneratorExtension codeGeneratorExtension =
                GradleCompatibility.createExtensionWithObjectFactory(project, Constants.SCHEMA_DESIGN_EXTENSION_NAME, CodeGeneratorExtension.class);
        project.getTasks().withType(GenerateAvroJavaTask.class).configureEach(task -> {
            task.setResourceAvroDirPath(codeGeneratorExtension.getResourceAvroDirPath().get());
            task.getOutputCharacterEncoding().convention(codeGeneratorExtension.getOutputCharacterEncoding());
            task.getStringType().convention(codeGeneratorExtension.getStringType());
            task.getFieldVisibility().convention(codeGeneratorExtension.getFieldVisibility());
            task.getTemplateDirectory().convention(codeGeneratorExtension.getTemplateDirectory());
            task.isCreateSetters().convention(codeGeneratorExtension.isCreateSetters());
            task.isCreateOptionalGetters().convention(codeGeneratorExtension.isCreateOptionalGetters());
            task.isGettersReturnOptional().convention(codeGeneratorExtension.isGettersReturnOptional());
            task.isOptionalGettersForNullableFieldsOnly().convention(codeGeneratorExtension.isOptionalGettersForNullableFieldsOnly());
            task.isEnableDecimalLogicalType().convention(codeGeneratorExtension.isEnableDecimalLogicalType());
            task.getLogicalTypeFactories().convention(codeGeneratorExtension.getLogicalTypeFactories());
            task.getCustomConversions().convention(codeGeneratorExtension.getCustomConversions());
        });
        return codeGeneratorExtension;
    }

    private static void configureTasks(final Project project, ClearAvroJavaTask clearAvroJavaTask) {
        ProjectUtils.getSourceSets(project).configureEach(sourceSet -> {
            TaskProvider<GenerateAvroJavaTask> protoTaskProvider = configureJavaGenerationTask(project, sourceSet, clearAvroJavaTask);
            configureTaskDependencies(project, sourceSet, protoTaskProvider);
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

    private static TaskProvider<GenerateAvroJavaTask> configureJavaGenerationTask(final Project project, final SourceSet sourceSet, ClearAvroJavaTask clearAvroJavaTask) {
        String taskName = sourceSet.getTaskName("generate", "avroJava");
        Set<File> srcDirs = sourceSet.getResources().getSrcDirs();
        final File[] avroDir = {null};
        TaskProvider<GenerateAvroJavaTask> javaTaskProvider = project.getTasks().register(taskName, GenerateAvroJavaTask.class, task -> {
            task.setDescription(String.format("Generates %s Avro Java source files from schema/protocol definition files.",
                    sourceSet.getName()));
            task.setGroup(Constants.GROUP_SOURCE_GENERATION);
            avroDir[0] = new File(task.getResourceAvroDirPath().get() + File.separator + project.getName());
            srcDirs.add(avroDir[0]);
            if (SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName())) {
//                task.source(ProjectUtils.getAvroSourceDir(project, sourceSet));
                task.source(avroDir[0], clearAvroJavaTask);
            }
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
        if (avroDir[0] != null) {
            srcDirs.remove(avroDir[0]);
        }
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
