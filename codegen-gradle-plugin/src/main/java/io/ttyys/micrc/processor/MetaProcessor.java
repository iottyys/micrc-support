package io.ttyys.micrc.processor;

import com.squareup.javapoet.JavaFile;
import com.sun.tools.javac.code.Symbol;
import io.ttyys.micrc.annotations.MessageMeta;
import io.ttyys.micrc.annotations.TypeMeta;
import io.ttyys.micrc.processor.tools.ClassGeneratedUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.File;
import java.util.*;

@SupportedAnnotationTypes({
        "io.ttyys.micrc.annotations.TypeMeta",
        "io.ttyys.micrc.annotations.MessageMeta"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MetaProcessor extends AbstractProcessor {

    private final Map<Symbol.ClassSymbol, List<Symbol.MethodSymbol>> classSymbolMap = new HashMap<>(0);
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        try {
            for (Element element : env.getElementsAnnotatedWith(TypeMeta.class)) {
                if (element.getKind() != ElementKind.INTERFACE) {
                    this.error(element, TypeMeta.class.getSimpleName());
                    return true;
                }
                classSymbolMap.put(((Symbol.ClassSymbol) element), new ArrayList<>(0));
//                this.processTypeMeta(element);
            }

            for (Element element : env.getElementsAnnotatedWith(MessageMeta.class)) {
                if (element.getKind() != ElementKind.METHOD) {
                    this.error(element, MessageMeta.class.getSimpleName());
                    return true;
                }
                Symbol.MethodSymbol methodSymbol = ((Symbol.MethodSymbol) element);
                for (Map.Entry<Symbol.ClassSymbol, List<Symbol.MethodSymbol>> classSymbol : classSymbolMap.entrySet()) {
                    if (classSymbol.getKey().toString().equals(methodSymbol.owner.toString())) {
                        classSymbol.getValue().add(methodSymbol);
                        break;
                    }
                }
            }
            String path = this.filer.getResource(StandardLocation.SOURCE_OUTPUT, "com", "szyk").toUri().getPath();
            for (Map.Entry<Symbol.ClassSymbol, List<Symbol.MethodSymbol>> classSymbol : classSymbolMap.entrySet()) {
                JavaFile javaFile = ClassGeneratedUtils.generateJava(classSymbol);
                javaFile.writeTo(new File(path));
            }

            return false;
        } catch (Exception e) {
            throw new IllegalStateException("could not process algorithm sub. ", e);
        }
    }

    private void error(Element e, Object... args) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                String.format("only support interface with annotation @%S", args), e);
    }
}
