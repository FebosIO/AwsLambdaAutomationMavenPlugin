package io.febos.development.plugins.febos.maven.permisos;

import com.google.gson.Gson;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * blah blah blah
 *
 * @goal validate-security
 * @phase package
 * @requiresDependencyResolution compile+runtime
 */
@Mojo(name = "permisos", requiresProject = true,requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
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
    @Parameter(defaultValue = "cl", readonly = true, required = true)
    public String pais;

    @Parameter(defaultValue = "desarrollo,pruebas,certificacion,produccion", readonly = true, required = true)
    public String ambientes;

    /**
     * Scans the source code for this module to look for instances of the
     * annotations we're looking for.
     *
     * @throws MojoExecutionException thrown if there are errors during analysis
     */
    public void execute() throws MojoExecutionException {
        try {
            getLog().info("CONFIGURAMOS ");
            Gson g = new Gson();
            getLog().info("pais  " + pais);
            getLog().info("ambientes  " + ambientes);
            getLog().info("projectClasspathElements  " + projectClasspathElements);
            getLog().info("annotations " + g.toJson(annotations));
//            getLog().info("project " + g.toJson(project));
        } catch (Exception e) {

        }
        // There's nothing to do if there are no annotations configured.
        if (annotations == null || annotations.isEmpty()) {
            annotations=new ArrayList<>();
            PermisoAnnotation permiso= new PermisoAnnotation();
            permiso.setName("io.febos.core.validador.Permiso");
            annotations.add(permiso);
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
        AtomicInteger processedCount = new AtomicInteger();
        annotations.parallelStream().forEach(sa -> {
            initAnnotationType(projectClassloader, sa);
            if (sa.getClazz() != null) {
                try {
                    processedCount.addAndGet(validateAllAnnotationExpressions(reflections, sa));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return processedCount.get();
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
            casses.parallelStream().forEach(c -> {
                getLog().debug(String.format("anotated " + pais + " : %s", c));
                procesarPermiso(pais, c, annoType);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return processedCount;
    }

    private void procesarPermiso(String pais, Class<?> clase, Class<? extends Annotation> tipoAnotacion) {
        getLog().debug(String.format("Validando permisos en  " + pais + " : %s  para %s", clase, tipoAnotacion));
        Annotation anotacion = clase.getAnnotation(tipoAnotacion);
        Method[] metods = tipoAnotacion.getDeclaredMethods();
        HashMap<String, String> valores = new HashMap<>();
        for (int i = 0; i < metods.length; i++) {
            try {
                getLog().debug(String.format("valor de %s  :  %s", metods[i].getName(), metods[i].invoke(anotacion)));
                valores.put(metods[i].getName().trim(), String.valueOf(metods[i].invoke(anotacion)));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        valores.put("pais", pais);
        String requestPermisos = new Gson().toJson(valores);
        getLog().info("REQUEST PERMISOS   " + requestPermisos);
        asignarPermisos(ambientes, valores);
    }

    public void asignarPermisos(String ambientes, HashMap<String, String> req) {
        String server = "";
        String[] paises = req.get("pais").split(",");
        for (int i = 0; i < paises.length; i++) {
            String pais = paises[i];
            switch (pais) {
                case "co": {
                    server = "colombia-cluster.cluster-c3m0bwpzpiz8.us-east-1.rds.amazonaws.com";
                    break;
                }
                case "cl": {
                    server = "febos-io-chile.cluster-c3m0bwpzpiz8.us-east-1.rds.amazonaws.com";
                    break;
                }
                case "legacy": {
                    server = "febosb.c3m0bwpzpiz8.us-east-1.rds.amazonaws.com";
                    break;
                }
                default: {
                    throw new RuntimeException("Pais no permitido o soportado");
                }
            }

            String url = "jdbc:mysql://" + server + ":3306";
            try (Connection conn = DriverManager.getConnection(url, "superadmin", "ia$olution$**")) {
                conn.setAutoCommit(true);
                String[] ambientesAr = ambientes.split(",");
                for (String ambiente : ambientesAr) {
                    String insert = "INSERT IGNORE INTO febos_" + ambiente + ".permisos\n" +
                            "(codigo, accion, nombre, descripcion, nivel, actualizado)\n" +
                            "VALUES(?, ?, ?, ?, ?, ?);";
                    try (Statement st = conn.prepareStatement(insert)) {
                        ((PreparedStatement) st).setString(1, req.get("codigo"));
                        ((PreparedStatement) st).setString(2, req.get("accion"));
                        ((PreparedStatement) st).setString(3, req.get("nombre"));
                        ((PreparedStatement) st).setString(4, req.get("descripcion"));
                        ((PreparedStatement) st).setString(5, req.get("nivel"));
                        ((PreparedStatement) st).setDate(6, new java.sql.Date(new java.util.Date().getTime()));
                        getLog().info("PERMISO " + st.toString());
                        ((PreparedStatement) st).executeUpdate();
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

    }

}
