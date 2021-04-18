package io.ttyys.micrc.codegen.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CodeGeneratorPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        // 通用配置
        // 注解处理器配置
        // 引入其它插件，客户端只引入这个插件，可以具备所有能力
    }
}
