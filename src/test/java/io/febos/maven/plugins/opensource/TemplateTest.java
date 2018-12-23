package io.febos.maven.plugins.opensource;

import io.febos.maven.plugins.opensource.Template;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TemplateTest {

    @Test
    @Ignore
    public void test() {
        Map<String, String> template = new HashMap<>();
        template.put("describe","$input.params('describe')");
        template.put("stage","$stageVariables.ambiente");
        template.put("token","$input.params('token')");
        template.put("empresa","$input.params('empresa')");
        template.put("grupo","$input.params('grupo')");
        template.put("debug","$input.params('debug')");
        template.put("simular","$input.params('simular')");
        template.put("ip","$context.identity.sourceIp");
        template.put("ambito","$input.params('ambito')");
        template.put("rutProveedor","$input.params('rutProveedor')");
        template.put("rutCliente","$input.params('rutCliente')");
        template.put("origen","$input.json('$.origen')");
        template.put("configuraciones","input.json('$.configuraciones[*]')");

        String json="{\n";
        String xml="{\n";
        Template t = new Template(template,json,xml).invoke();
        json = t.getTmplJson();

        System.out.printf(json);
    }

    @Test
    @Ignore
    public void testFile() {
        Template t = new Template(new File("/Users/claudio/IdeaProjects/Febos3-Backend/colombia/lambdas/co_intercambio_registrar/src/main/resources/template.vm")).invoke();
        String json = t.getTmplJson();

        System.out.printf(json);
    }

}