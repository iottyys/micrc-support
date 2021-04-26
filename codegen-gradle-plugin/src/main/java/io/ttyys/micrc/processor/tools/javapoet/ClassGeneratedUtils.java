package io.ttyys.micrc.processor.tools.javapoet;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.ttyys.micrc.processor.tools.javapoet.dto.AdapterAnnotation;
import io.ttyys.micrc.processor.tools.javapoet.dto.AdapterClass;
import io.ttyys.micrc.processor.tools.javapoet.dto.AdapterMethod;
import io.ttyys.micrc.processor.tools.javapoet.dto.AdapterMethodParam;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassGeneratedUtils {
    /**
     * 构造接口实现类
     *
     * @param adapterClass 待实现类信息
     * @return 实现类
     */
    public static JavaFile generateJava(AdapterClass adapterClass) {
        // 类名-公共-实现接口
        TypeSpec.Builder classBuild = TypeSpec.classBuilder(adapterClass.getClassName());
        classBuild.addModifiers(Modifier.PUBLIC).addSuperinterface(adapterClass.getInterfaceName());
        // 添加注解列表
        classBuild.addAnnotations(createAnnotationList(adapterClass.getAnnotationList()));
        // 添加 @Autowire 属性

        // 添加方法列表
        classBuild.addMethods(createMethodList(adapterClass));

        return JavaFile.builder(adapterClass.getPkg(), classBuild.build()).build();
    }

    /**
     * 构造方法列表
     *
     * @param adapterClass 待构造方法信息类
     * @return 方法列表
     */
    private static List<MethodSpec> createMethodList(AdapterClass adapterClass) {
        List<MethodSpec> methodSpecList = new ArrayList<>(0);
        for (AdapterMethod adapterMethod : adapterClass.getMethodList()) {
            // 方法
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(adapterMethod.getName());
            methodBuilder.addModifiers(Modifier.PUBLIC);
            // 方法参数
            for (AdapterMethodParam param : adapterMethod.getParamList()) {
                methodBuilder.addParameter(param.getClazz(), param.getName());
            }
            // 方法注解列表
            methodBuilder.addAnnotations(createAnnotationList(adapterMethod.getAnnotationList()));
            // 方法返回值
            Class<?> returnClass = adapterMethod.getReturnClass();
            if (returnClass != void.class) {
                methodBuilder.addStatement(adapterMethod.getReturnStatement());
            }
            methodBuilder.returns(returnClass);
            methodSpecList.add(methodBuilder.build());
        }
        return methodSpecList;
    }

    /**
     * 构造注解列表
     *
     * @param annotationList 待构建注解信息列表
     * @return 构造的注解列表
     */
    private static List<AnnotationSpec> createAnnotationList(List<AdapterAnnotation> annotationList) {
        List<AnnotationSpec> annotationSpecList = new ArrayList<>(0);
        for (AdapterAnnotation adapterAnnotation : annotationList) {
            AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(adapterAnnotation.getClazz());
            Map<String, Object> annotationParamMap = adapterAnnotation.getParams();
            if (!annotationParamMap.isEmpty()) {
                for (Map.Entry<String, Object> paramEntry : annotationParamMap.entrySet()) {
                    annotationBuilder.addMember(paramEntry.getKey(), "$S", paramEntry.getValue());
                }
            }
            annotationSpecList.add(annotationBuilder.build());
        }
        return annotationSpecList;
    }

    /**
     * 构造类型对应的默认值字符串
     *
     * @param clazz 类型
     * @return 默认值字符串
     */
    public static String getDefaultValueByClass(Class<?> clazz) {
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

    /**
     * 根据类型字符串获取类型
     *
     * @param typeStr 类型字符串
     * @return 类型
     */
    public static Class<?> getTypeByStr(String typeStr) {
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
