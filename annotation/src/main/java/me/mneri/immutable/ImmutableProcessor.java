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
import java.util.List;
import java.util.Set;

public class ImmutableProcessor extends AbstractProcessor {
    private List<String> getStronglyImmutableStandardClassNames() {
        //@formatter:off
        return Arrays.asList(
                BigDecimal.class.getCanonicalName(),
                BigInteger.class.getCanonicalName(),
                Boolean   .class.getCanonicalName(),
                Byte      .class.getCanonicalName(),
                Character .class.getCanonicalName(),
                Double    .class.getCanonicalName(),
                Float     .class.getCanonicalName(),
                Integer   .class.getCanonicalName(),
                Long      .class.getCanonicalName(),
                Short     .class.getCanonicalName(),
                String    .class.getCanonicalName()
        );
        //@formatter:on
    }

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

    private TypeElement getType(VariableElement variable) {
        return (TypeElement) getTypeUtils().asElement(variable.asType());
    }

    private Types getTypeUtils() {
        return processingEnv.getTypeUtils();
    }

    private List<String> getWeaklyImmutableStandardClassNames() {
        //@formatter:off
        return Arrays.asList(
                Object.class.getCanonicalName(),
                Number.class.getCanonicalName()
        );
        //@formatter:on
    }

    private boolean isAnnotatedWith(TypeElement element, Class<? extends Annotation> clazz) {
        return element.getAnnotation(clazz) != null;
    }

    private boolean isFinal(Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    private boolean isPrimitive(VariableElement variable) {
        return variable.asType().getKind().isPrimitive();
    }

    private boolean isStronglyImmutable(TypeElement type) {
        //@formatter:off
        return getStronglyImmutableStandardClassNames().contains(type.getQualifiedName().toString()) ||
               (isAnnotatedWith(type, Immutable.class) && isFinal(type));
        //@formatter:on
    }

    private boolean isWeaklyImmutable(TypeElement type) {
        //@formatter:off
        return getWeaklyImmutableStandardClassNames().contains(type.getQualifiedName().toString()) ||
               isAnnotatedWith(type, Immutable.class);
        //@formatter:on
    }

    private void printError(String format, Object... params) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(format, params));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Immutable.class)) {
            processType((TypeElement) element);
        }

        return true;
    }

    private void processField(TypeElement container, VariableElement field) {
        if (!isFinal(field)) {
            String format = "%s is annotated with @Immutable but field '%s' is not final.";
            printError(format, container.getQualifiedName(), field.getSimpleName());
        }

        if (!isPrimitive(field) && !isStronglyImmutable(getType(field))) {
            String format = "%s is annotated with @Immutable but field '%s' is not primitive or strongly immutable.";
            printError(format, container.getQualifiedName(), field.getSimpleName());
        }
    }

    private void processType(TypeElement type) {
        TypeElement superType = getSuperType(type);

        if (!isWeaklyImmutable(superType)) {
            String format = "%s is annotated with @Immutable but its super class is not weakly immutable.";
            printError(format, type.getQualifiedName(), type.getQualifiedName());
        }

        for (Element element : type.getEnclosedElements()) {
            if (!(element instanceof VariableElement)) {
                continue;
            }

            processField(type, (VariableElement) element);
        }
    }
}