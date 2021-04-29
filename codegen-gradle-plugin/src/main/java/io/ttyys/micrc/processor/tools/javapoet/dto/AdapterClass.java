package io.ttyys.micrc.processor.tools.javapoet.dto;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import io.ttyys.micrc.annotations.Structure;
import io.ttyys.micrc.annotations.logic.LogicCustom;

import java.util.ArrayList;
import java.util.List;

/**
 * 适配器生成时需要的类配置
 */
public class AdapterClass {
    private String pkg;
    private Type interfaceName;
    private String className;
    private List<AdapterAnnotation> annotationList;
    private List<AdapterMethod> methodList;
    private boolean hasCustom = false;

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
        if (annotationList == null) {
            annotationList = new ArrayList<>(0);
        }
        return annotationList;
    }

    public void setAnnotationList(List<AdapterAnnotation> annotationList) {
        this.annotationList = annotationList;
    }

    public List<AdapterMethod> getMethodList() {
        if (methodList == null) {
            methodList = new ArrayList<>(0);
        }
        return methodList;
    }

    public void setMethodList(List<AdapterMethod> methodList) {
        this.methodList = methodList;
    }

    public AdapterClass addAnnotation(AdapterAnnotation annotation) {
        getAnnotationList().add(annotation);
        return this;
    }

    public AdapterClass addMethod(AdapterMethod method) {
        if (!hasCustom) {
            for (AdapterAnnotation adapterAnnotation : method.getAnnotationList()) {
                if (adapterAnnotation.getClazz() == LogicCustom.class) {
                    hasCustom = true;
                    break;
                }
            }
        }
        getMethodList().add(method);
        return this;
    }

    public boolean isHasCustom() {
        return hasCustom;
    }
}
