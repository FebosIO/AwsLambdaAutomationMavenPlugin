package io.febos.maven.plugins.opensource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

public class Template {
    private Map<String, String> template;
    private String tmplJson;
    private String tmplXml;
    private File templateFile;

    public Template(Map<String, String> template, String tmplJson, String tmplXml) {
        this.template = template;
        this.tmplJson = tmplJson;
        this.tmplXml = tmplXml;
    }

    public Template(Map<String, String> template) {
        this.template = template;
        this.tmplJson = "{\n";
        this.tmplXml = "{\n";
    }

    public Template(File mappingFile, String tmplJson, String tmplXml) {
        templateFile = mappingFile;
    }

    public Template(File mappingFile) {
        templateFile = mappingFile;
    }

    public String getTmplJson() {
        return tmplJson;
    }

    public String getTmplXml() {
        return tmplXml;
    }

    public Template invoke() {
        if (template != null) {
            return getTemplateFromMap();
        } else {
            return getTemplateFromFile();
        }
    }

    private Template getTemplateFromFile() {
        try {
            Logger.getGlobal().info("CARGAMOS TEMPLATE DESDE ARCHIVO");
            String jsonTemplate = new String(Files.readAllBytes(Paths.get(templateFile.getAbsolutePath())));
            tmplJson = jsonTemplate;
            tmplXml = jsonTemplate;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    protected Template getTemplateFromMap() {
        int item = 1;
        for (Map.Entry<String, String> parametro : template.entrySet()) {
            String envoltorio = parametro.getValue().contains(".json(") ? "" : "\"";

            tmplJson += "  \"" + parametro.getKey() + "\":" + envoltorio + parametro.getValue();
            if (!envoltorio.isEmpty()) {
                tmplXml += "  \"" + parametro.getKey() + "\":" + envoltorio + parametro.getValue();
            }

            if (item == template.size()) {
                if (!envoltorio.isEmpty()) {
                    tmplXml += envoltorio + "\n}";
                } else {
                    tmplXml += "\"xmlBodyComoEntrada\":\"$util.base64Encode($input.json('$'))\"\n}";
                }
                tmplJson += envoltorio + "\n}";//fin del json
            } else {
                tmplJson += envoltorio + ",\n";
                if (!envoltorio.isEmpty()) {
                    tmplXml += envoltorio + ",\n";
                }
            }
            item++;
        }
        return this;
    }
}