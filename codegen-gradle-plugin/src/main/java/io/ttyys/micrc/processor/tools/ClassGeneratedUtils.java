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
                methodBuilder.addStatement("return null");
            }
            methodBuilder.returns(returnClass);
            classBuild.addMethod(methodBuilder.build());
        }
        return JavaFile.builder(classSymbol.owner.toString() + ".impl", classBuild.build()).build();
    }

    private static Class<?> getTypeByStr(String typeStr) {
        Class<?> clazz;
        switch (typeStr) {
            case "boolean":
                clazz = Boolean.class;
                break;
            case "char":
                clazz = Character.class;
                break;
            case "float":
                clazz = Float.class;
                break;
            case "double":
                clazz = Double.class;
                break;
            case "byte":
                clazz = Byte.class;
                break;
            case "short":
                clazz = Short.class;
                break;
            case "int":
                clazz = Integer.class;
                break;
            case "long":
                clazz = Long.class;
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
