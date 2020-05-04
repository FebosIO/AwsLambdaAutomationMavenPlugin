package io.febos.development.plugins.febos.maven.permisos;

import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
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
@Mojo(name = "permisos", requiresProject = true, requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
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
    @Parameter(name = "deployFilterPermiso",property = "deployFilterPermiso")
    public String deployFilterPermiso;
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
            getLog().info("project " + project.getArtifactId());
        } catch (Exception e) {

        }
        getLog().warn("SE CREARA USUARIO DE DB " + project.getArtifactId());
        crearCredencialesBd(project.getArtifactId());
        // There's nothing to do if there are no annotations configured.
        if (annotations == null || annotations.isEmpty()) {
            annotations = new ArrayList<>();
            PermisoAnnotation permiso = new PermisoAnnotation();
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

    public void crearCredencialesBd(String nombre) {
        try {
            final AWSLambdaAsync cliente = AWSLambdaAsyncClientBuilder.standard().withRegion("us-east-1").build();
            String pais = "";

            if (nombre.startsWith("io_")) {
                pais = "cl,co";
            }
            if (nombre.startsWith("cl_")) {
                pais = "cl";
            }
            if (nombre.startsWith("co_")) {
                pais = "co";
            }
            getLog().info("Procesando paices "+pais);
            for (String paisX : pais.split(",")) {
                String server = "febosb.c3m0bwpzpiz8.us-east-1.rds.amazonaws.com";
                InvokeRequest invokeRequest = new InvokeRequest();
                String lambdaCreaUser = "";
                if (paisX.equalsIgnoreCase("co")) {
                    lambdaCreaUser = "io_config_ioco_db_lambda";
                    server = "colombia-cluster.cluster-c3m0bwpzpiz8.us-east-1.rds.amazonaws.com";
                }
                if (nombre.startsWith("io_")) {
                    lambdaCreaUser = "io_config_ioco_db_lambda";
                }
                if (paisX.equalsIgnoreCase("cl")) {
                    lambdaCreaUser = "io_config_cl_db_lambda";
                    server = "febos-io-chile.cluster-c3m0bwpzpiz8.us-east-1.rds.amazonaws.com";
                }
                if (nombre.contains("_legacy_")) {
                    lambdaCreaUser = "io_config_cl_legacy_db_lambda";
                    server = "febosb.c3m0bwpzpiz8.us-east-1.rds.amazonaws.com";
                }

                getLog().info("Creando usuario en base de datos  " + nombre + "" + paisX + " " + server);
                procesar(nombre, server);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void procesar(String lambda, String server) {

        // Logica del lambda io_config_db_lambda
        String url = "jdbc:mysql://" + server + ":3306";
        String usuario = generarNombreUsuarioLambda(lambda);
        try (Connection conn = DriverManager.getConnection(url, "superadmin", "ia$olution$**")) {
            conn.setAutoCommit(true);
            System.out.println("CREATE USER " + usuario + "  " + server);
            String crearUsuario = "CREATE USER '" + usuario + "'@'%' IDENTIFIED BY 'ia$olution$**';";
            Statement st = conn.createStatement();
            st.execute(crearUsuario);
            st.close();
        } catch (SQLException ex) {

        }
        try (Connection conn = DriverManager.getConnection(url, "superadmin", "ia$olution$**")) {
            conn.setAutoCommit(true);
            System.out.println("GRANT USER " + usuario);
            String permisos = "GRANT SELECT, INSERT, DELETE, UPDATE ,EXECUTE ON *.* TO " + usuario + "@'%' IDENTIFIED BY 'ia$olution$**';";
            Statement st = conn.createStatement();
            st.execute(permisos);
            st.close();
            st = conn.createStatement();
            st.execute("FLUSH PRIVILEGES;");
            st.close();
        } catch (SQLException ex) {

        }

    }


    public static String generarNombreUsuarioLambda(String lambda) {
        String usuario = lambda.replace(lambda.split("_")[0] + "_", "");
        usuario = usuario.replaceAll("a", "").replaceAll("e", "").replaceAll("i", "").replaceAll("o", "").replaceAll("u", "");
        try {
            usuario = usuario.substring(0, 15);
        } catch (Exception e) {
        }
        ;
        return usuario;
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
        String codigo = req.get("codigo");
        if(deployFilterPermiso!= null && !deployFilterPermiso.trim().isEmpty() && !codigo.trim().contains(deployFilterPermiso)){
            return;
        }
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
