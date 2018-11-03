package me.mneri.immutable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

public class ImmutableProcessor extends AbstractProcessor {
    private TypeElement getSuperType(TypeElement type) {
        return (TypeElement) processingEnv.getTypeUtils().asElement(type.getSuperclass());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Immutable.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    private boolean isAnnotatedWith(Element element, Class<? extends Annotation> clazz) {
        return element.getAnnotation(clazz) != null;
    }

    private boolean isDirectSubTypeOf(TypeElement type, Class<?> clazz) {
        return getSuperType(type).getQualifiedName().contentEquals(clazz.getCanonicalName());
    }

    private boolean isFinal(Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    private void printError(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Immutable.class)) {
            if (!(element instanceof TypeElement)) {
                continue;
            }

            process((TypeElement) element);
        }

        return true;
    }

    private void process(TypeElement type) {
        if (!isDirectSubTypeOf(type, Object.class) || !isAnnotatedWith(getSuperType(type), Immutable.class)) {
            printError(String.format("Class %s should be java.lang.Object direct subtype.", type.getQualifiedName()));
        }

        for (Element element : type.getEnclosedElements()) {
            if (!(element instanceof VariableElement)) {
                continue;
            }

            process((VariableElement) element);
        }
    }

    private void process(VariableElement variable) {
        if (!isFinal(variable)) {
            printError(String.format("Field %s should be final.", variable.getSimpleName()));
        }

        if (!isAnnotatedWith(variable, Immutable.class)) {
            printError(String.format("%s type is not immutable", variable.getSimpleName()));
        }
    }
}