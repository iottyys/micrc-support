package io.ttyys.micrc.processor.tools.javapoet.dto;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.sun.tools.javac.code.Symbol;
import io.ttyys.micrc.annotations.logic.LogicCustom;
import io.ttyys.micrc.processor.tools.javapoet.ClassGeneratedUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 适配器生成时需要的方法配置
 */
public class AdapterMethod {
    private String name;
    private List<AdapterAnnotation> annotationList;
    private List<AdapterMethodParam> paramList;
    private TypeName returnClass;
    private String returnStatement;

    public AdapterMethod(Symbol.MethodSymbol methodSymbol) {
        name = methodSymbol.name.toString();
        for (Symbol.VarSymbol parameter : methodSymbol.getParameters()) {
            addParam(new AdapterMethodParam(ClassName.get(parameter.type), parameter.getSimpleName().toString()));
        }
        LogicCustom logicCustom = methodSymbol.getAnnotation(LogicCustom.class);
        if (logicCustom != null) {
            addAnnotation(new AdapterAnnotation(LogicCustom.class));
        }
        addAnnotation(new AdapterAnnotation(Override.class));
        returnClass = ClassName.get(methodSymbol.getReturnType());
        if (returnClass != TypeName.VOID) {
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
        if (paramList == null) {
            paramList = new ArrayList<>(0);
        }
        return paramList;
    }

    public AdapterMethod addParam(AdapterMethodParam param) {
        getParamList().add(param);
        return this;
    }

    public void setParamList(List<AdapterMethodParam> paramList) {
        this.paramList = paramList;
    }

    public List<AdapterAnnotation> getAnnotationList() {
        if (annotationList == null) {
            annotationList = new ArrayList<>(0);
        }
        return annotationList;
    }

    public AdapterMethod addAnnotation(AdapterAnnotation annotation) {
        getAnnotationList().add(annotation);
        return this;
    }

    public void setAnnotationList(List<AdapterAnnotation> annotationList) {
        this.annotationList = annotationList;
    }

    public TypeName getReturnClass() {
        return returnClass;
    }

    public void setReturnClass(TypeName returnClass) {
        this.returnClass = returnClass;
    }

    public String getReturnStatement() {
        return returnStatement;
    }

    public void setReturnStatement(String returnStatement) {
        this.returnStatement = returnStatement;
    }
}
