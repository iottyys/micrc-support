package io.ttyys.micrc.processor.tools.javapoet.dto;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import io.ttyys.micrc.annotations.Structure;
import io.ttyys.micrc.codegen.gradle.plugin.common.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 适配器生成时需要的类配置
 */
public class AdapterClass {
    private String pkg;
    private Type interfaceName;
    private String className;
    private List<AdapterAnnotation> annotationList = new ArrayList<>(0);
    private List<AdapterMethod> methodList = new ArrayList<>(0);

    public AdapterClass(Symbol.ClassSymbol classSymbol) {
        Structure structure = classSymbol.getAnnotation(Structure.class);
        interfaceName = classSymbol.type;
        pkg = interfaceName.toString().split(structure.interfacePkg())[0] + structure.implPkg();
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public Type getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(Type interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<AdapterAnnotation> getAnnotationList() {
        return annotationList;
    }

    public void setAnnotationList(List<AdapterAnnotation> annotationList) {
        this.annotationList = annotationList;
    }

    public List<AdapterMethod> getMethodList() {
        return methodList;
    }

    public void setMethodList(List<AdapterMethod> methodList) {
        this.methodList = methodList;
    }
}
