package me.mneri.immutable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class ImmutableProcessor extends AbstractProcessor {
    private TypeElement getSuperType(TypeElement type) {
        return (TypeElement) getTypeUtils().asElement(type.getSuperclass());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Immutable.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    private Types getTypeUtils() {
        return processingEnv.getTypeUtils();
    }

    private boolean isAnnotatedWith(TypeElement element, Class<? extends Annotation> clazz) {
        return element.getAnnotation(clazz) != null;
    }

    private boolean isDirectSubTypeOf(TypeElement type, Class<?> clazz) {
        return getSuperType(type).getQualifiedName().toString().equals(clazz.getCanonicalName());
    }

    private boolean isEffectivelyImmutable(VariableElement variable) {
        //@formatter:off
        return Arrays.asList(
                BigDecimal.class.getCanonicalName(),
                BigInteger.class.getCanonicalName(),
                Boolean   .class.getCanonicalName(),
                Byte      .class.getCanonicalName(),
                Character .class.getCanonicalName(),
                Double    .class.getCanonicalName(),
                Integer   .class.getCanonicalName(),
                Long      .class.getCanonicalName(),
                Short     .class.getCanonicalName(),
                String    .class.getCanonicalName()
        ).contains(variable.asType().toString());
        //@formatter:on
    }

    private boolean isFinal(Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    private boolean isImmutable(VariableElement variable) {
        TypeElement type = (TypeElement) getTypeUtils().asElement(variable.asType());
        return isAnnotatedWith(type, Immutable.class) || isEffectivelyImmutable(variable);
    }

    private boolean isPrimitive(VariableElement variable) {
        return variable.asType().getKind().isPrimitive();
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
        if (!isDirectSubTypeOf(type, Object.class) && !isAnnotatedWith(getSuperType(type), Immutable.class)) {
            printError(String.format("Class %s's super type should be @Immutable or java.lang.Object.", type.getQualifiedName()));
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

        if (!isPrimitive(variable) && !isImmutable(variable)) {
            printError(String.format("%s should be primitive, immutable or effectively immutable.", variable.getSimpleName()));
        }
    }
}