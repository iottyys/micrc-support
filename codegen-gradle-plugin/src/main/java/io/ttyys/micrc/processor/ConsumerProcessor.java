package io.ttyys.micrc.processor;

import com.squareup.javapoet.JavaFile;
import com.sun.tools.javac.code.Symbol;
import io.ttyys.micrc.annotations.logic.LogicCustom;
import io.ttyys.micrc.annotations.logic.LogicDelegate;
import io.ttyys.micrc.annotations.logic.LogicIntegration;
import io.ttyys.micrc.annotations.technology.InformationConsumer;
import io.ttyys.micrc.annotations.technology.LocalTransferConsumer;
import io.ttyys.micrc.annotations.technology.RpcTransferConsumer;
import io.ttyys.micrc.codegen.gradle.plugin.common.Constants;
import io.ttyys.micrc.processor.tools.javapoet.ClassGeneratedUtils;
import io.ttyys.micrc.processor.tools.javapoet.dto.AdapterAnnotation;
import io.ttyys.micrc.processor.tools.javapoet.dto.AdapterClass;
import io.ttyys.micrc.processor.tools.javapoet.dto.AdapterMethod;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "io.ttyys.micrc.annotations.technology.LocalTransferConsumer",
        "io.ttyys.micrc.annotations.technology.RpcTransferConsumer",
        "io.ttyys.micrc.annotations.technology.InformationConsumer",
        "io.ttyys.micrc.annotations.technology.LogicCustom",
        "io.ttyys.micrc.annotations.technology.LogicDelegate",
        "io.ttyys.micrc.annotations.technology.LogicIntegration"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ConsumerProcessor extends AbstractProcessor {

    private Filer filer;
    private Path projectDir;
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.projectDir = this.findDir();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        try {
            List<AdapterClass> adapterClassList = interceptInterfaceAdapter(env);
            populateAdapterMessageList(env, adapterClassList);
            // 没有自定义实现逻辑的实现类,放在此源代码包
            File annotationProcessMainFile = new File(projectDir.toFile(), "build/generated/sources/annotationProcessor/java/main");
            // 有自定义实现逻辑的实现类,放在此源代码包
            File srcMainFile = new File(projectDir.toFile(), "src/main/java");
            for (AdapterClass adapterClass : adapterClassList) {
                JavaFile javaFile = ClassGeneratedUtils.generateJava(adapterClass);
                if (adapterClass.isHasCustom()) {
                    existsJava(srcMainFile, javaFile);
                } else {
                    javaFile.writeTo(annotationProcessMainFile);
                }
            }
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("could not process algorithm sub. ", e);
        }
    }

    private void existsJava(File path, JavaFile javaFile) throws IOException {
        String javaPath = javaFile.packageName.replaceAll("\\.", File.separator) + File.separator;
        String fileName = javaFile.typeSpec.name + Constants.NAMESPACE_SEPARATOR + Constants.JAVA_EXTENSION_NAME;
        File oldJavaFile = new File(path, javaPath + fileName);
        // 若有自定义实现的逻辑把原有文件重命名备份,重新生成新文件
        if (oldJavaFile.exists()) {
            String newFileName = javaFile.typeSpec.name + format.format(new Date()) + Constants.NAMESPACE_SEPARATOR + Constants.TXT_EXTENSION_NAME;
            //noinspection ResultOfMethodCallIgnored
            oldJavaFile.renameTo(new File(path, javaPath + newFileName));
        }
        javaFile.writeTo(path);
    }

    /**
     * 拦截技术接口
     *
     * @return 构造的技术接口Adapter 列表
     */
    private List<AdapterClass> interceptInterfaceAdapter(RoundEnvironment env) {
        Set<Element> allInterfaceSet = new HashSet<Element>(0);
        allInterfaceSet.addAll(env.getElementsAnnotatedWith(LocalTransferConsumer.class));
        allInterfaceSet.addAll(env.getElementsAnnotatedWith(RpcTransferConsumer.class));
        allInterfaceSet.addAll(env.getElementsAnnotatedWith(InformationConsumer.class));
        return allInterfaceSet.stream().filter(element -> {
            if (element.getKind() != ElementKind.INTERFACE) {
                this.error(element, element.getSimpleName());
                return false;
            }
            return true;
        }).map(element -> {
            Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) element;
            AdapterClass adapterClass = new AdapterClass(classSymbol);
            adapterClass.setClassName(getAnnotationByInterface(classSymbol));
            adapterClass.addAnnotation(new AdapterAnnotation(org.springframework.stereotype.Component.class));
            return adapterClass;
        }).collect(Collectors.toList());
    }

    /**
     * 构造的Adapter的方法: 填充消息列表
     */
    private void populateAdapterMessageList(RoundEnvironment env, List<AdapterClass> adapterClassList) {
        Set<Element> allMethodSet = new HashSet<Element>(0);
        allMethodSet.addAll(env.getElementsAnnotatedWith(LogicCustom.class));
        allMethodSet.addAll(env.getElementsAnnotatedWith(LogicDelegate.class));
        allMethodSet.addAll(env.getElementsAnnotatedWith(LogicIntegration.class));
        allMethodSet.stream().filter(element -> {
            if (element.getKind() != ElementKind.METHOD) {
                this.error(element, element.getSimpleName());
                return false;
            }
            return true;
        }).forEach(element -> {
            Symbol.MethodSymbol methodSymbol = ((Symbol.MethodSymbol) element);
            for (AdapterClass adapterClass : adapterClassList) {
                if (adapterClass.getInterfaceName().toString().equals(methodSymbol.owner.toString())) {
                    adapterClass.addMethod(new AdapterMethod(methodSymbol));
                    break;
                }
            }
        });
    }

    private String getAnnotationByInterface(Symbol.ClassSymbol classSymbol) {
        String className;
        LocalTransferConsumer localTransferConsumer = classSymbol.getAnnotation(LocalTransferConsumer.class);
        if (localTransferConsumer != null) {
            className = localTransferConsumer.adapterClassName();
        } else {
            RpcTransferConsumer rpcTransferConsumer = classSymbol.getAnnotation(RpcTransferConsumer.class);
            if (rpcTransferConsumer != null) {
                className = rpcTransferConsumer.adapterClassName();
            } else {
                InformationConsumer informationConsumer = classSymbol.getAnnotation(InformationConsumer.class);
                if (informationConsumer != null) {
                    className = informationConsumer.adapterClassName();
                } else {
                    throw new RuntimeException("Unknown name of class.");
                }
            }
        }
        return className;
    }


    private void error(Element e, Object... args) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                String.format("only support interface with annotation @%S", args), e);
    }

    private Path findDir() {
        String tmp = "tmp";
        try {
            URI uri = this.filer.getResource(StandardLocation.CLASS_OUTPUT, "", tmp).toUri();
            return Paths.get(uri).getParent().getParent().getParent().getParent().getParent();
        } catch (IOException e) {
            throw new IllegalStateException("unable to find resources dir. ", e);
        }
    }
}
