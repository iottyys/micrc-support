package io.ttyys.micrc.processor;

import io.ttyys.micrc.annotations.Structure;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes({"io.ttyys.micrc.annotations.Structure"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class StructureProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        try {
            for (Element element : env.getElementsAnnotatedWith(Structure.class)) {
                if (element.getKind() != ElementKind.INTERFACE) {
                    this.error(element, Structure.class.getSimpleName());
                    return true;
                }
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
