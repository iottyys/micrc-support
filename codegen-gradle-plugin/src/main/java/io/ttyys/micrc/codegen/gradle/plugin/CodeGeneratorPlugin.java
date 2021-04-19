package io.ttyys.micrc.codegen.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.plugins.PluginContainer;

public class CodeGeneratorPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        /* buildScript 块
        ScriptHandler scriptHandler = project.getBuildscript();
        DependencyHandler scriptDependencyHandler = scriptHandler.getDependencies();
        scriptDependencyHandler.add("classpath", scriptDependencyHandler.create("io.ttyys.micrc:codegen-gradle-plugin:0.0.1-SNAPSHOT"));

        PluginContainer pluginContainer = project.getPlugins();
        pluginContainer.apply("code-generator");
        pluginContainer.apply("service-integration");*/

        DependencyHandler dependencyHandler  = project.getDependencies();
        dependencyHandler.add("implementation", dependencyHandler.create("org.apache.avro:avro-compiler:1.10.2"));
        dependencyHandler.add("compile", dependencyHandler.create("io.ttyys.micrc:micrc-annotations:0.0.1-SNAPSHOT"));
        // 通用配置
        // 注解处理器配置
        // 引入其它插件，客户端只引入这个插件，可以具备所有能力
    }
}
