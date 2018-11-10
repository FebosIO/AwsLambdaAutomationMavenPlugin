/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.febos.development.plugins.febos.maven.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Michel M. <michel@febos.cl>
 */
public class Main {

    public static final HashMap<String, String> datos = new HashMap<>();

    public static void main(String[] args) throws Exception {
        cargarCSV("/Users/michel/Desktop/lambdas.csv");

        List<File> poms = buscarPoms("/Users/michel/Code/Febos3-Backend/colombia/lambdas");
        for (File pom : poms) {

            try {
                System.out.println(pom.getAbsolutePath());

                //procesarArchivo(pom.getAbsolutePath());
                actualizarAPI(pom.getAbsolutePath());
                System.out.println(" -> OK!");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(" -> ERROR!");
            }
        }
        //procesarArchivo("/Users/michel/Code/Febos3-Backend/global/librerias/io_local_queue/pom.xml");
    }

    private static void cargarCSV(String csv) throws Exception {
        String content = new String(Files.readAllBytes(new File(csv).toPath()));
        String[] lineas = content.split("\n");
        for (String linea : lineas) {
            String[] campos = linea.split(",");
            if (campos[3].equalsIgnoreCase("SIN API") || campos[3].equalsIgnoreCase("ELIMINADO")) {
                continue;
            }
            datos.put("co_" + campos[0], campos[1] + "," + campos[2] + "," + campos[3]);
        }
    }

    public static void actualizarAPI(String ruta) throws Exception {
        File archivo = new File(ruta);
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.parse(ruta);
        String artifactId = doc.getElementsByTagName("artifactId").item(0).getTextContent().trim();
        String[] campos;
        try {
            campos = datos.get(artifactId).split(",");
        } catch (Exception e) {
            System.out.println(" -> NO TIENE CONFIG REVISADA");
            return;
        }
        doc.getElementsByTagName("febos.permiso.codigo").item(0).setTextContent(campos[0]);
        doc.getElementsByTagName("resource").item(0).setTextContent(campos[1]);
        doc.getElementsByTagName("metodo").item(0).setTextContent(campos[2].toUpperCase());
        doc.getElementsByTagName("api").item(0).setTextContent("jbd5aoglqj");
        guardar(doc, archivo);
    }

    public static void procesarArchivo(String ruta) {
        try {
            File archivo = new File(ruta);
            int versionActual = 1;
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.parse(archivo);
            int version;
            if (doc.getElementsByTagName("febos.config.version").getLength() == 0) {
                version = 0;
            } else {
                version = Integer.parseInt(doc.getElementsByTagName("febos.config.version").item(0).getTextContent());
            }
            if (versionActual > version) {
                switch (version) {
                    case 0:
                        pasarVersion1(doc, archivo);
                        System.out.println("Proyecto actualizado: " + doc.getElementsByTagName("artifactId").item(0).getTextContent());
                }
            } else {
                System.out.println("Proyecto ya estaba actualizado: " + doc.getElementsByTagName("artifactId").item(0).getTextContent());
            }

        } catch (IOException | NumberFormatException | ParserConfigurationException | DOMException | SAXException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Ups!");
        }
    }

    public static void pasarVersion1(Document doc, File archivo) {
        boolean esLambda = doc.getElementsByTagName("apiGateway").getLength() == 1;
        if (esLambda) {
            eliminarReportingAntiguo(doc);
            eliminarCredencialesAws(doc);
            crearNuevaConfigGlobal(doc);
            crearConfigFindBugs(doc);
            eliminarActualConfiguracionDeExecucionFindBugs(doc);
            crearEjecucionFindBugs(doc);
        } else {
            crearNuevaConfigGlobal(doc);
            eliminarReportingAntiguo(doc);
            eliminarCredencialesAws(doc);
            configurarPomLibreria(doc);
        }
        guardar(doc, archivo);
    }

    private static void eliminarReportingAntiguo(Document doc) {
        Element reporting = (Element) doc.getElementsByTagName("reporting").item(0);
        try {
            reporting.getParentNode().removeChild(reporting);
        } catch (Exception e) {

        }
    }

    public static void crearNuevaConfigGlobal(Document doc) {
        Element properties = (Element) doc.getElementsByTagName("properties").item(0);
        Element opcionEspecial = doc.createElement("dependency.locations.enabled");
        opcionEspecial.setTextContent("false");
        properties.appendChild(opcionEspecial);
        Element configVersion = doc.createElement("febos.config.version");
        configVersion.setTextContent("1");
        properties.appendChild(configVersion);
    }

    public static void crearConfigFindBugs(Document doc) {
        Element febosMavenPlugin = buscarFebosMavenPlugin(doc);

        Element plugin = doc.createElement("plugin");

        Element group = doc.createElement("groupId");
        group.setTextContent("org.codehaus.mojo");
        plugin.appendChild(group);

        Element artifact = doc.createElement("artifactId");
        artifact.setTextContent("findbugs-maven-plugin");
        plugin.appendChild(artifact);

        Element version = doc.createElement("version");
        version.setTextContent("3.0.4");
        plugin.appendChild(version);

        Element config = doc.createElement("configuration");
        plugin.appendChild(config);

        Element xmlOutput = doc.createElement("xmlOutput");
        xmlOutput.setTextContent("true");
        config.appendChild(xmlOutput);

        Element xmlOutputDirectory = doc.createElement("xmlOutputDirectory");
        xmlOutputDirectory.setTextContent("${project.basedir}/target");
        config.appendChild(xmlOutputDirectory);

        Element executions = doc.createElement("executions");
        plugin.appendChild(executions);

        Element execution = doc.createElement("execution");
        executions.appendChild(execution);

        Element phase = doc.createElement("phase");
        phase.setTextContent("verify");
        execution.appendChild(phase);

        Element goals = doc.createElement("goals");
        execution.appendChild(goals);

        Element goal = doc.createElement("goal");
        goal.setTextContent("findbugs");
        goals.appendChild(goal);

        febosMavenPlugin.getParentNode().insertBefore(plugin, febosMavenPlugin);
    }

    public static void crearEjecucionFindBugs(Document doc) {
        Element febosMavenPlugin = buscarFebosMavenPlugin(doc);
        Element ejecucionLabdaApi = (Element) febosMavenPlugin.getElementsByTagName("lambda").item(0).getParentNode().getParentNode();

        Element execution = doc.createElement("execution");

        Element id = doc.createElement("id");
        id.setTextContent("analisis-de-bugs");
        execution.appendChild(id);

        Element phase = doc.createElement("phase");
        phase.setTextContent("install");
        execution.appendChild(phase);

        Element goals = doc.createElement("goals");
        execution.appendChild(goals);

        Element goal = doc.createElement("goal");
        goal.setTextContent("deploy-bugs");
        goals.appendChild(goal);

        Element config = doc.createElement("configuration");
        execution.appendChild(config);

        Element proyecto = doc.createElement("proyecto");
        proyecto.setTextContent("${project.artifactId}");
        config.appendChild(proyecto);

        Element pais = doc.createElement("pais");
        pais.setTextContent("${febos.codigo.pais}");
        config.appendChild(pais);

        Element grupo = doc.createElement("grupo");
        grupo.setTextContent("${project.groupId}");
        config.appendChild(grupo);

        Element version = doc.createElement("version");
        version.setTextContent("${project.version}");
        config.appendChild(version);

        Element descripcion = doc.createElement("descripcion");
        descripcion.setTextContent("${project.description}");
        config.appendChild(descripcion);

        Element rutaXml = doc.createElement("rutaXml");
        rutaXml.setTextContent("${project.basedir}/target");
        config.appendChild(rutaXml);

        Element rutaXsl = doc.createElement("rutaXsl");
        rutaXsl.setTextContent("${project.basedir}/../../../global/archivos/findbugs/layout.xsl");
        config.appendChild(rutaXsl);

        ejecucionLabdaApi.getParentNode().insertBefore(execution, ejecucionLabdaApi);
    }

    private static void eliminarCredencialesAws(Document doc) {
        NodeList cr = doc.getElementsByTagName("credencialesAWS");
        List<Element> credenciales = new ArrayList<>();
        for (int i = 0; i < cr.getLength(); i++) {
            credenciales.add((Element) cr.item(i));
        }
        try {
            credenciales.forEach((el) -> {
                el.getParentNode().removeChild(el);
            });
        } catch (DOMException e) {

        }
    }

    private static void eliminarActualConfiguracionDeExecucionFindBugs(Document doc) {
        //analisis-de-bugs
        try {
            NodeList ejecuciones = doc.getElementsByTagName("execution");
            Element ejecucionParaEliminar;
            for (int i = 0; i < ejecuciones.getLength(); i++) {
                Element ejecucion = (Element) ejecuciones.item(i);
                Element id = (Element) ejecucion.getElementsByTagName("id").item(0);
                if (id != null && id.getTextContent().equals("analisis-de-bugs")) {
                    ejecucionParaEliminar = ejecucion;
                    ejecucionParaEliminar.
                            getParentNode().
                            removeChild(ejecucionParaEliminar);
                    break;
                }
            }
        } catch (DOMException e) {
            e.printStackTrace();
        }

    }

    private static Element buscarFebosMavenPlugin(Document doc) {
        NodeList plugins = doc.getElementsByTagName("plugin");
        for (int i = 0; i < plugins.getLength(); i++) {
            Element plugin = (Element) plugins.item(i);
            String artifact = plugin.getElementsByTagName("artifactId").item(0).getTextContent();
            if (artifact.equals("febos-maven-plugin")) {
                return plugin;
            }
        }
        return null;
    }

    private static void guardar(Document doc, File archivo) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new PrintWriter(archivo));
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            //String xmlString = result.getWriter().toString();
            //System.out.println(xmlString);
        } catch (FileNotFoundException | IllegalArgumentException | TransformerException ex) {
            throw new RuntimeException("No se pudo convertir y guarda POM.xml: " + archivo.getAbsolutePath());
        }
    }

    public static List<File> buscarPoms(String ruta) {
        List<File> poms = new ArrayList<>();
        File root = new File(ruta);
        File[] archivos = root.listFiles();
        for (File archivo : archivos) {
            if (archivo.isDirectory()) {
                poms.addAll(buscarPoms(archivo.getAbsolutePath()));
            } else {
                if (archivo.getName().equals("pom.xml")) {
                    poms.add(archivo);
                }
            }
        }
        return poms;
    }

    private static void configurarPomLibreria(Document doc) {
        try {
            Element build = (Element) doc.getElementsByTagName("build").item(0);
            if (build != null) {
                build.getParentNode().removeChild(build);
            }

            String xml = "<build>\n"
                    + "        <plugins>\n"
                    + "            <plugin>\n"
                    + "                <groupId>org.codehaus.mojo</groupId>\n"
                    + "                <artifactId>findbugs-maven-plugin</artifactId>\n"
                    + "                <version>3.0.4</version>\n"
                    + "                <configuration>\n"
                    + "                    <xmlOutput>true</xmlOutput>\n"
                    + "                    <xmlOutputDirectory>${project.basedir}/target</xmlOutputDirectory>\n"
                    + "                </configuration>\n"
                    + "                <executions>\n"
                    + "                    <execution>\n"
                    + "                        <phase>verify</phase> \n"
                    + "                        <goals>\n"
                    + "                            <goal>findbugs</goal> \n"
                    + "                        </goals>\n"
                    + "                    </execution>\n"
                    + "                </executions>\n"
                    + "            </plugin>\n"
                    + "            <plugin>\n"
                    + "                <groupId>io.febos.development.plugins</groupId>\n"
                    + "                <artifactId>febos-maven-plugin</artifactId>\n"
                    + "                <version>1.0-SNAPSHOT</version>\n"
                    + "                <executions>\n"
                    + "                    <execution>\n"
                    + "                        <id>analisis-de-bugs</id>\n"
                    + "                        <phase>install</phase>\n"
                    + "                        <goals>\n"
                    + "                            <goal>deploy-bugs</goal>\n"
                    + "                        </goals>\n"
                    + "                        <configuration>\n"
                    + "                            <proyecto>${project.artifactId}</proyecto>\n"
                    + "                            <pais>${febos.codigo.pais}</pais>\n"
                    + "                            <grupo>${project.groupId}</grupo>\n"
                    + "                            <version>${project.version}</version>\n"
                    + "                            <descripcion>${project.description}</descripcion>\n"
                    + "                            <rutaXml>${project.basedir}/target</rutaXml>\n"
                    + "                            <rutaXsl>${project.basedir}/../../../global/archivos/findbugs/layout.xsl</rutaXsl>\n"
                    + "                        </configuration>\n"
                    + "                    </execution>\n"
                    + "                </executions>\n"
                    + "            </plugin>\n"
                    + "        </plugins>\n"
                    + "    </build>";
            Element node = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes()))
                    .getDocumentElement();
            Node buildImportada = doc.importNode(node, true);
            Element proyecto = (Element) doc.getElementsByTagName("project").item(0);
            proyecto.appendChild(buildImportada);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

}
