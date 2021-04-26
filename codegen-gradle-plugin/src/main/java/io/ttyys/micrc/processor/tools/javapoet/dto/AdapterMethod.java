package io.ttyys.micrc.processor.tools.javapoet.dto;

import com.sun.tools.javac.code.Symbol;
import io.ttyys.micrc.processor.tools.javapoet.ClassGeneratedUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 适配器生成时需要的方法配置
 */
public class AdapterMethod {
    private String name;
    private List<AdapterAnnotation> annotationList = new ArrayList<>(0);
    private List<AdapterMethodParam> paramList = new ArrayList<>(0);
    private Class<?> returnClass;
    private String returnStatement;

    public AdapterMethod(Symbol.MethodSymbol methodSymbol) {
        name = methodSymbol.name.toString();
        for (Symbol.VarSymbol parameter : methodSymbol.getParameters()) {
            paramList.add(new AdapterMethodParam(ClassGeneratedUtils.getTypeByStr(parameter.type.toString()), parameter.getSimpleName().toString()));
        }
        annotationList.add(new AdapterAnnotation(java.lang.Override.class));
        returnClass = ClassGeneratedUtils.getTypeByStr(methodSymbol.getReturnType().toString());
        if (returnClass != void.class) {
            returnStatement = "return " + ClassGeneratedUtils.getDefaultValueByClass(returnClass);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AdapterMethodParam> getParamList() {
        return paramList;
    }

    public void setParamList(List<AdapterMethodParam> paramList) {
        this.paramList = paramList;
    }

    public List<AdapterAnnotation> getAnnotationList() {
        return annotationList;
    }

    public void setAnnotationList(List<AdapterAnnotation> annotationList) {
        this.annotationList = annotationList;
    }

    public Class<?> getReturnClass() {
        return returnClass;
    }

    public void setReturnClass(Class<?> returnClass) {
        this.returnClass = returnClass;
    }

    public String getReturnStatement() {
        return returnStatement;
    }

    public void setReturnStatement(String returnStatement) {
        this.returnStatement = returnStatement;
    }
}
