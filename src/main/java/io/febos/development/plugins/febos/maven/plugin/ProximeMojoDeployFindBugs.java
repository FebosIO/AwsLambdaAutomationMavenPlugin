/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.febos.development.plugins.febos.maven.plugin;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author Michel M. <michel@febos.cl>
 */
@Mojo(name = "deploy-bugs")
public class ProximeMojoDeployFindBugs extends AbstractMojo {

    @Parameter(defaultValue = "")
    String proyecto;

    @Parameter(defaultValue = "")
    String pais;

    @Parameter(defaultValue = "")
    String descripcion;

    @Parameter(defaultValue = "")
    String grupo;

    @Parameter(defaultValue = "")
    String version;

    @Parameter(defaultValue = "")
    String credencialesAWS;

    @Parameter(defaultValue = "")
    String rutaXml;

    @Parameter(defaultValue = "")
    String rutaXsl;

    @Parameter(defaultValue = "archivos.febos.io/temporal")
    String rutaS3;

    CustomCredentialsProvider credenciales;
    AmazonS3 s3client;
    private Document proyectos;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        s3client = AmazonS3ClientBuilder.defaultClient();
      //  cargarBaseDeDatos();
        /*String nombre = proyecto + "_" + version + ".html";
        String bucket = rutaS3.split("/")[0];
        String s3path = rutaS3.substring(rutaS3.indexOf("/") + 1);
        s3path += s3path.endsWith("/") ? "" : "/";
        s3path += "analisis/" + nombre;
        rutaXml += s3path.endsWith("/") ? "" : "/";
        String rutaHtml = rutaXml + nombre;

        rutaXml += "findbugsXml.xml";
        getLog().info("Generando reporte de bugs");

        try {
            File stylesheet = new File(rutaXsl);
            File datafile = new File(rutaXml);
            getLog().info("ruta del xsl: " + stylesheet.getAbsolutePath());
            TransformerFactory factory = TransformerFactory.newInstance();
            Templates xsl = factory.newTemplates(new StreamSource(stylesheet));
            Transformer transformer = xsl.newTransformer();
            Source text = new StreamSource(datafile);
            transformer.transform(text, new StreamResult(new File(rutaHtml)));
            s3client.putObject(new PutObjectRequest("febos-io", "builds/reportes/" + proyecto + "_" + version + ".html", new File(rutaHtml)));
            s3client.setObjectAcl("febos-io", "builds/reportes/" + proyecto + "_" + version + ".html", CannedAccessControlList.PublicRead);
            getLog().info("Reporte generado!");

        } catch (Exception e) {
            e.printStackTrace();
        }
        String bandera=":earth_americas: ";
        if(grupo!=null && grupo.startsWith("co.")){
            bandera=":flag-co: ";
        }else if(grupo!=null && grupo.startsWith("cl.")){
            bandera=":flag-cl: ";
        }

        if (proyecto != null && !proyecto.isEmpty()) {
            getLog().info("Actualizando base de datos de proyectos Febos");
            if (existeProyecto(proyecto, version)) {
                actualizarProyecto(proyecto, version, obtenerEstadisticas(rutaHtml));
            } else {
                crearProyecto(proyecto, grupo, descripcion, pais, version, obtenerEstadisticas(rutaHtml));
            }
            subirBaseDeDatosActualizada();
            sendSlackMessage(bandera+"*" + obtenerUsuarioGit() + "* ha subido una nueva versión al ambiente de *desarrollo* del lambda `" + grupo + "::" + proyecto + "` (" + descripcion + "), puedes ver el análisis de bugs <https://s3.amazonaws.com/febos-io/builds/reportes/" + proyecto + "_" + version + ".html|::haciendo click aqui::>");
        } else {
            getLog().info("*** ESTE PROYECTO NECESITA ACTUALIZAR SU POM.XML PARA HACER REVISION DE BUGS!");
            sendSlackMessage(bandera+"*" + obtenerUsuarioGit() + "* ha subido una nueva versión al ambiente de *desarrollo* del lambda `" + grupo + "::" + proyecto + "` (" + descripcion + "), pero no tiene configurado el analisis de bugs, *CONFIGURAR LO ANTES POSIBLE!!*");
        }
        */
    }

    public void cargarBaseDeDatos() {
        String bdComoString = s3client.getObjectAsString("febos-io", "builds/proyectos.xml");
        proyectos = convertirStringEnDocument(bdComoString);
    }

    public void actualizarProyecto(String nombre, String version, HashMap<String, String> estadisticas) {
        NodeList listado = proyectos.getElementsByTagName("proyecto");
        int cantidad = listado.getLength();
        for (int i = 0; i < cantidad; i++) {
            Element proyecto = (Element) listado.item(i);
            String nombreProyecto = proyecto.getElementsByTagName("nombre").item(0).getTextContent();
            String versionProyecto = proyecto.getElementsByTagName("version").item(0).getTextContent();
            if (nombre.equals(nombreProyecto) && version.equals(versionProyecto)) {
                
                if (proyecto.getElementsByTagName("total").getLength() == 0) {
                    Element tagOtros = proyectos.createElement("otros");
                    Element tagLeves = proyectos.createElement("leves");
                    Element tagErrores = proyectos.createElement("errores");
                    Element tagCriticos = proyectos.createElement("criticos");
                    Element tagTotal = proyectos.createElement("total");
                    tagOtros.setTextContent(estadisticas.get("OTROS"));
                    tagLeves.setTextContent(estadisticas.get("LEVES"));
                    tagErrores.setTextContent(estadisticas.get("ERROR"));
                    tagCriticos.setTextContent(estadisticas.get("CRITICO"));
                    tagTotal.setTextContent(estadisticas.get("TOTAL"));
                    proyecto.appendChild(tagOtros);
                    proyecto.appendChild(tagLeves);
                    proyecto.appendChild(tagErrores);
                    proyecto.appendChild(tagCriticos);
                    proyecto.appendChild(tagTotal);
                }else{
                    proyecto.getElementsByTagName("otros").item(0).setTextContent(estadisticas.get("OTROS"));
                    proyecto.getElementsByTagName("leves").item(0).setTextContent(estadisticas.get("LEVES"));
                    proyecto.getElementsByTagName("errores").item(0).setTextContent(estadisticas.get("ERROR"));
                    proyecto.getElementsByTagName("criticos").item(0).setTextContent(estadisticas.get("CRITICO"));
                    proyecto.getElementsByTagName("total").item(0).setTextContent(estadisticas.get("TOTAL"));
                }
                proyecto.getElementsByTagName("actualizacion").item(0).setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            }
        }

    }

    public void crearProyecto(String nombre, String grupo, String descripcion, String pais, String version, HashMap<String, String> estadisticas) {
        Element tagProyecto = proyectos.createElement("proyecto");

        Element tagNombre = proyectos.createElement("nombre");
        Element tagGrupo = proyectos.createElement("grupo");
        Element tagDescripcion = proyectos.createElement("descripcion");
        Element tagPais = proyectos.createElement("pais");
        Element tagVersion = proyectos.createElement("version");
        Element tagActualizacion = proyectos.createElement("actualizacion");
        //estadisticas
        Element tagOtros = proyectos.createElement("otros");
        Element tagLeves = proyectos.createElement("leves");
        Element tagErrores = proyectos.createElement("errores");
        Element tagCriticos = proyectos.createElement("criticos");
        Element tagTotal = proyectos.createElement("total");

        tagNombre.setTextContent(nombre);
        tagGrupo.setTextContent(grupo);
        tagDescripcion.setTextContent(descripcion);
        tagPais.setTextContent(pais);
        tagVersion.setTextContent(version);
        tagActualizacion.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        tagOtros.setTextContent(estadisticas.get("OTROS"));
        tagLeves.setTextContent(estadisticas.get("LEVES"));
        tagErrores.setTextContent(estadisticas.get("ERROR"));
        tagCriticos.setTextContent(estadisticas.get("CRITICO"));
        tagTotal.setTextContent(estadisticas.get("TOTAL"));

        tagProyecto.appendChild(tagNombre);
        tagProyecto.appendChild(tagGrupo);
        tagProyecto.appendChild(tagDescripcion);
        tagProyecto.appendChild(tagPais);
        tagProyecto.appendChild(tagVersion);
        tagProyecto.appendChild(tagActualizacion);
        tagProyecto.appendChild(tagOtros);
        tagProyecto.appendChild(tagLeves);
        tagProyecto.appendChild(tagErrores);
        tagProyecto.appendChild(tagCriticos);
        tagProyecto.appendChild(tagTotal);

        proyectos.getElementsByTagName("proyectos").item(0).appendChild(tagProyecto);

    }

    public boolean existeProyecto(String nombre, String version) {
        NodeList listado = proyectos.getElementsByTagName("proyecto");
        int cantidad = listado.getLength();
        for (int i = 0; i < cantidad; i++) {
            Element proyecto = (Element) listado.item(i);
            String nombreProyecto = proyecto.getElementsByTagName("nombre").item(0).getTextContent();
            String versionProyecto = proyecto.getElementsByTagName("version").item(0).getTextContent();
            if (nombre.equals(nombreProyecto) && version.equals(versionProyecto)) {
                return true;
            }
        }
        return false;
    }

    public static String obtenerUsuarioGit() {
        try {
            String comando = "git config user.name";
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(comando);
            pr.waitFor();
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = null;
            String nombre = "";
            try {
                while ((line = input.readLine()) != null) {
                    nombre += line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return nombre;
        } catch (Exception ex) {
            return "Alguien misterioso";
        }

    }

    public static Document convertirStringEnDocument(String doc) {
        DocumentBuilder db;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(doc));
            return db.parse(is);
        } catch (Exception ex) {
            Logger.getLogger(ProximeMojoDeployFindBugs.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Imposible recuperar base de datos de proyectos");
        }
    }

    public static String convertirDocumentEnString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    private void subirBaseDeDatosActualizada() {
        String contenido = convertirDocumentEnString(proyectos);
        InputStream stream = new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8));
        s3client.putObject(new PutObjectRequest("febos-io", "builds/proyectos.xml", stream, null));
        s3client.setObjectAcl("febos-io", "builds/proyectos.xml", CannedAccessControlList.PublicRead);

        //s3client.putObject(new PutObjectRequest("febos-io", "builds/reportes/"+proyecto+"_"+version+".html", stream,null));
    }

    public static void main(String[] args) throws Exception {
        //sendSlackMessage("prueba");
        //String html=new String(Files.readAllBytes(Paths.get("/Users/michel/Code/Febos3-Backend/global/librerias/io_core/target/6be4b85d-f447-48b3-8fe0-e8e81c6f2dfd.html")));

    }

    private static void sendSlackMessage(String message) {
        String urlRequest = "https://hooks.slack.com/services/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";
        JSONObject json = new JSONObject();
        try {
            json.put("channel", "febos_hola_mundo");
            json.put("text", message);
            json.put("username", "Asius");
            json.put("icon_emoji", ":robot_face:");

            String urlParameters = json.toString(0);
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;

            URL url = new URL(urlRequest);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setUseCaches(false);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
            } catch (IOException ex) {
                Logger.getLogger(ProximeMojoDeployFindBugs.class.getName()).log(Level.SEVERE, null, ex);
            }

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
            }
            conn.getResponseCode();
        } catch (JSONException e) {
            System.out.println("JSONException posting to Slack " + e);
        } catch (IllegalArgumentException e) {
            System.out.println("IllegalArgumentException posting to Slack " + e);
        } catch (MalformedURLException ex) {
            Logger.getLogger(ProximeMojoDeployFindBugs.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ProtocolException ex) {
            Logger.getLogger(ProximeMojoDeployFindBugs.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ProximeMojoDeployFindBugs.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static HashMap<String, String> obtenerEstadisticas(String rutahtml) {
        try {
            String[] contenido = new String(Files.readAllBytes(Paths.get(rutahtml))).split("\n");
            HashMap<String, String> r = new HashMap<>();
            int i = 0;
            for (String linea : contenido) {
                if (linea.contains("CONTADORES_RESUMEN")) {
                    r.put("OTROS", contenido[i + 1].trim().substring(contenido[i + 1].trim().indexOf("=") + 1, contenido[i + 1].trim().indexOf(";")));
                    r.put("LEVES", contenido[i + 2].trim().substring(contenido[i + 2].trim().indexOf("=") + 1, contenido[i + 2].trim().indexOf(";")));
                    r.put("ERROR", contenido[i + 3].trim().substring(contenido[i + 3].trim().indexOf("=") + 1, contenido[i + 3].trim().indexOf(";")));
                    r.put("CRITICO", contenido[i + 4].trim().substring(contenido[i + 4].trim().indexOf("=") + 1, contenido[i + 4].trim().indexOf(";")));
                    r.put("TOTAL", contenido[i + 5].trim().substring(contenido[i + 5].trim().indexOf("=") + 1, contenido[i + 5].trim().indexOf(";")));
                    return r;
                }
                i++;
            }
            return r;
        } catch (IOException ex) {
            Logger.getLogger(ProximeMojoDeployFindBugs.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
