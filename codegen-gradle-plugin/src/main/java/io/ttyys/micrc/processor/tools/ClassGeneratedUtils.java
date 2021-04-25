package io.ttyys.micrc.processor.tools;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.code.Symbol;
import io.ttyys.micrc.annotations.MessageMeta;
import io.ttyys.micrc.annotations.TypeMeta;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Map;

public class ClassGeneratedUtils {
    public static JavaFile generateJava(Map.Entry<Symbol.ClassSymbol, List<Symbol.MethodSymbol>> classSymbolEntry) {
        List<Symbol.MethodSymbol> methodSymbolList = classSymbolEntry.getValue();
        Symbol.ClassSymbol classSymbol = classSymbolEntry.getKey();
        TypeSpec.Builder classBuild = TypeSpec.classBuilder(classSymbol.name.toString() + "Impl");
        classBuild.addModifiers(Modifier.PUBLIC).addSuperinterface(classSymbol.type);
        TypeMeta typeMeta = classSymbol.getAnnotation(TypeMeta.class);
        classBuild.addAnnotation(AnnotationSpec.builder(getTypeByStr(typeMeta.value())).build());
        classBuild.addAnnotation(AnnotationSpec.builder(getTypeByStr(typeMeta.mapping())).addMember("value", "$S", typeMeta.url()).build());
        for (Symbol.MethodSymbol methodSymbol : methodSymbolList) {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodSymbol.name.toString());
            methodBuilder.addModifiers(Modifier.PUBLIC);
            for (Symbol.VarSymbol param : methodSymbol.getParameters()) {
                methodBuilder.addParameter(getTypeByStr(param.type.toString()), param.getSimpleName().toString());
            }
            MessageMeta messageMeta = methodSymbol.getAnnotation(MessageMeta.class);
            methodBuilder.addAnnotation(AnnotationSpec.builder(getTypeByStr(messageMeta.mapping())).addMember("value", "$S", messageMeta.url()).build());
            Class<?> returnClass = getTypeByStr(methodSymbol.getReturnType().toString());
            if (returnClass != void.class) {
                methodBuilder.addStatement("return " + getDefaultValueByClass(returnClass));
            }
            methodBuilder.returns(returnClass);
            classBuild.addMethod(methodBuilder.build());
        }
        return JavaFile.builder(classSymbol.owner.toString() + ".impl", classBuild.build()).build();
    }

    /**
     * 构造类型对应的默认值字符串
     * @param clazz 类型
     * @return 默认值字符串
     */
    private static String getDefaultValueByClass(Class<?> clazz) {
        String defaultValue = "";
        if (boolean.class.equals(clazz)) {
            defaultValue = "false";
        } else if (char.class.equals(clazz)) {
            defaultValue = "' '";
        } else if (float.class.equals(clazz)) {
            defaultValue = "0f";
        } else if (double.class.equals(clazz)) {
            defaultValue = "0D";
        } else if (byte.class.equals(clazz) || short.class.equals(clazz) || int.class.equals(clazz)) {
            defaultValue = "0";
        } else if (long.class.equals(clazz)) {
            defaultValue = "0L";
        } else {
            defaultValue = "null";
        }
        return defaultValue;
    }
    private short a() {
        return 0;
    }

    /**
     * 根据类型字符串获取类型
     * @param typeStr 类型字符串
     * @return 类型
     */
    private static Class<?> getTypeByStr(String typeStr) {
        Class<?> clazz;
        switch (typeStr) {
            case "boolean":
                clazz = boolean.class;
                break;
            case "char":
                clazz = char.class;
                break;
            case "float":
                clazz = float.class;
                break;
            case "double":
                clazz = double.class;
                break;
            case "byte":
                clazz = byte.class;
                break;
            case "short":
                clazz = short.class;
                break;
            case "int":
                clazz = int.class;
                break;
            case "long":
                clazz = long.class;
                break;
            case "void":
                clazz = void.class;
                break;
            default:
                try {
                    clazz = Class.forName(typeStr);
                } catch (ClassNotFoundException e) {
                    clazz = String.class;
                }
                break;
        }
        return clazz;
    }
}
