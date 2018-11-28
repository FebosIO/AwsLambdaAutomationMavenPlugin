package io.febos.development.plugins.febos.maven.permisos;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mojo(name = "permisos", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        requiresDependencyResolution = ResolutionScope.TEST)
public class PermisoPlugin extends AbstractMojo {
    /**
     * Injected by maven to give us a reference to the project so we can get the
     * output directory and other properties.
     */
    @Component
    public MavenProject project;

    /**
     * List of annotations that we'll scan for.
     */
    @Parameter(property = "annotations", required = false)
    public List<PermisoAnnotation> annotations;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    public List<String> projectClasspathElements;

    /**
     * Scans the source code for this module to look for instances of the
     * annotations we're looking for.
     *
     * @throws MojoExecutionException thrown if there are errors during analysis
     */
    public void execute() throws MojoExecutionException {

        // There's nothing to do if there are no annotations configured.
        if (annotations == null || annotations.isEmpty()) {
            getLog().warn("There are no annotations configured so there's nothing for this plugin to do.");
            return;
        }

        int processedCount = 0;

        try {
            getLog().warn("project.getBuild().getOutputDirectory()  " + project.getBuild().getOutputDirectory());
            // these paths should include our module's source root plus any generated code
            List<String> compileSourceOutputs = Collections.singletonList(project.getBuild().getOutputDirectory());
            URL[] sourceFiles = buildMavenClasspath(compileSourceOutputs);

            // the project classpath includes the source roots plus the transitive
            // dependencies according to our Mojo annotation's requiresDependencyResolution
            // attribute.
            getLog().warn("this.projectClasspathElements  " + this.projectClasspathElements);

            URL[] projectClasspath = buildMavenClasspath(this.projectClasspathElements);
            getLog().info("CLASS LOADER CANT " + projectClasspath.length);

            URLClassLoader projectClassloader = new URLClassLoader(projectClasspath);
            Reflections reflections = new Reflections(
                    new ConfigurationBuilder()
                            .setUrls(sourceFiles)
                            .addClassLoaders(projectClassloader)
                            .setScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner())
//                    })
            );

            processedCount += processAnnotations(projectClassloader, reflections);
        } catch (Throwable e) {
            throw new MojoExecutionException("A fatal error occurred while validating Spel annotations," +
                    " see stack trace for details.", e);
        }
        getLog().info(String.format("Processed %d annotations", processedCount));

    }

    protected URL[] buildMavenClasspath(List<String> classpathElements) throws MojoExecutionException {
        List<URL> projectClasspathList = new ArrayList<>();
        for (String element : classpathElements) {
            try {
                projectClasspathList.add(new File(element).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new MojoExecutionException(element + " is an invalid classpath element", e);
            }
        }

        return projectClasspathList.toArray(new URL[projectClasspathList.size()]);
    }

    private int processAnnotations(URLClassLoader projectClassloader, Reflections reflections) throws Exception {
        int processedCount = 0;
        for (PermisoAnnotation sa : annotations) {
            initAnnotationType(projectClassloader, sa);
            if (sa.getClazz() != null) {
                processedCount += validateAllAnnotationExpressions(reflections, sa);
            }
        }
        return processedCount;
    }

    private void initAnnotationType(URLClassLoader projectClassloader, PermisoAnnotation sa) {
        if (sa.getClazz() == null) {
            try {
                //noinspection unchecked
                Class<? extends Annotation> clazz = (Class<? extends Annotation>) projectClassloader.loadClass(sa.getName());
                sa.setClazz(clazz);
                getLog().info(String.format("Loaded annotation %s", sa.getName()));
            } catch (Exception e) {
                getLog().warn("Could not find and instantiate class for annotation with name: " + sa.getName() + " " + projectClassloader.getURLs());
                getLog().error(e);
            }
        }

        if (sa.getExpressionRootClass() == null && sa.getExpressionRoot() != null) {
            try {
                //noinspection unchecked
                Class<?> clazz = projectClassloader.loadClass(sa.getExpressionRoot());
                sa.setExpressionRootClass(clazz);
                getLog().info(String.format("Loaded annotation expressionRoot %s", sa.getExpressionRoot()));
            } catch (Exception e) {
                getLog().warn("Could not find and instantiate class for annotation expressionRoot with name: " + sa.getExpressionRoot());
            }
        }
    }

    private int validateAllAnnotationExpressions(Reflections reflections, PermisoAnnotation sa) throws Exception {
        try {
            getLog().debug(String.format("Validating expression: %s", sa.getName()));
        } catch (Exception e) {
        }
        int processedCount = 0;
        Class<? extends Annotation> annoType = sa.getClazz();
        try {
            Set<Class<?>> casses = reflections.getTypesAnnotatedWith(annoType, true);
            getLog().debug(String.format("Validating expression class : %s", casses));
            for (Class<?> c : casses) {
                getLog().debug(String.format("anotated  : %s", c));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return processedCount;
    }

}
