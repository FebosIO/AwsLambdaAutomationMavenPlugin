package io.febos.development.plugins.febos.maven.permisos;

import java.lang.annotation.Annotation;

/**
 * Models the configuration of an annotation that we want to validate.
 * Each annotation includes the fully qualified name of the annotation and the
 * attribute that holds the spring expression.
 *
 * @author markford
 */
public class PermisoAnnotation {

    /**
     * The fully qualified name of the annotation that we're scanning for
     */
    private String name;

    /**
     * The name of the attribute on the annotation that contains the spring
     * expression.
     */
    private String attribute = "value";

    /**
     * Optional expression root against which we'll validate method calls.
     */
    private String expressionRoot;

    private Class<?> expressionRootClass;

    /**
     * Cached instance of the Annotation class.
     */
    private Class<? extends Annotation> clazz;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getExpressionRoot() {
        return expressionRoot;
    }

    public void setExpressionRoot(String expressionRoot) {
        this.expressionRoot = expressionRoot;
    }

    public Class<?> getExpressionRootClass() {
        return expressionRootClass;
    }

    public void setExpressionRootClass(Class<?> expressionRootClass) {
        this.expressionRootClass = expressionRootClass;
    }

    public Class<? extends Annotation> getClazz() {
        return clazz;
    }

    public void setClazz(Class<? extends Annotation> clazz) {
        this.clazz = clazz;
    }
}
