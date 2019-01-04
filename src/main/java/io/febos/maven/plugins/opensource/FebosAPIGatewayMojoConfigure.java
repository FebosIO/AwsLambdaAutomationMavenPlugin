/*
 * Copyright (C) IA Solutions Ltda - Todos los derechos reservados
 * Queda expresamente prohibida la copia o reproducción total o parcial de este archivo
 * sin el permiso expreso y por escrito de IA Solutions LTDA.
 * La detección de un uso no autorizado puede acarrear el inicio de acciones legales.
 */
package io.febos.maven.plugins.opensource;

import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.*;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.*;

/**
 * Maven Plugin para facilitar la configuración de API Gateway y Lambda. Permite
 * configurar el lmabda y api dateway desde el POM del proyecto maven.
 * @author Michel M. <michel@febos.cl>
 */
@Mojo(name = "api",requiresProject = true,requiresDirectInvocation = true)
public class FebosAPIGatewayMojoConfigure extends AbstractMojo {

    @Parameter
    boolean update;
    @Parameter
    boolean deleteJars;
    @Parameter(defaultValue = "")
    String credencialesAWS;
    @Parameter
    List<ApiGateway> endpoints;
    @Parameter
    Lambda lambda;
    @Parameter
    String accountId;
    @Parameter
    String region;

    @Parameter
    String stageDescriptor;

    CustomCredentialsProvider credenciales;
    public static AmazonS3 s3client;
    public static AWSLambda lambdaClient;
    public static AmazonApiGateway apiClient;
    HashMap<String, Boolean> recursos;
    HashMap<String, String> recursosId;
    HashMap<String, ArrayList<Method>> recursoMetodos;
    boolean lambdaNuevo;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        apiClient = AmazonApiGatewayClientBuilder.standard().withRegion(region).build();
        lambdaClient = AWSLambdaClientBuilder.standard().withRegion(region).build();
        try{
            if (endpoints != null) {
                if (lambdaNuevo) {
                    getLog().info("Configurando API Gateway para el nuevo lambda");
                } else {
                    getLog().info("Re-configurando API Gateway para el lambda");
                }
                for (ApiGateway gateway : endpoints) {
                    configurarApiGateway(gateway.api(),
                            gateway.resource(),
                            gateway.metodo(),
                            lambda.nombre(),
                            gateway.mapping(),
                            gateway.getMappingFile(),
                            gateway.getMappingFileResponse(),
                            gateway.headers,
                            region,
                            accountId
                    );
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al ejecutar plugin");
        }

    }

    protected void cargarResponseTemplate(String apiID, String resourceID, String verbo, File mappingFileResponse) {
        try {
            System.out.print("-> Configurando API para configurar respuestas");

            PutIntegrationResponseRequest pire = new PutIntegrationResponseRequest();
            Map<String, String> h = new HashMap<>();
//            h.put("method.response.header.Access-Control-Allow-Headers", "true");
//            h.put("method.response.header.Access-Control-Allow-Origin", "true");
//            h.put("method.response.header.Access-Control-Allow-Methods", "true");
            pire.setResponseParameters(h);
            pire.setHttpMethod(verbo);
            pire.setResourceId(resourceID);
            pire.setRestApiId(apiID);
            pire.setStatusCode("200");
            Map<String, String> velocityR = new HashMap<>();
            Template templateR = new Template(mappingFileResponse).invoke();
            velocityR.put("application/json", templateR.getTmplJson());
            pire.setResponseTemplates(velocityR);
            System.out.println(templateR.getTmplJson());
            apiClient.putIntegrationResponse(pire);
        } catch (Exception e) {
            getLog().warn("ERROR AL CONFIGURAR RESPUESTA");
            getLog().warn(e);
        }
    }


    public void configurarApiGateway(String apiID, String resourceID, String verbo, String lambdaName, Map<String, String> template, File mappingFile, File mappingFileResponse, String header, String region, String accountId) {
        if (apiID == null || apiID.isEmpty()) {
            return;
        }

        Map<String, String> emptyModels = new HashMap<>();

        emptyModels.put("application/json", "Empty");
        emptyModels.put("application/xml", "Empty");
        try {
            System.out.print("-> Eliminando configuración actual...");
            DeleteMethodRequest dmr = new DeleteMethodRequest();
            dmr.setHttpMethod(verbo);
            dmr.setResourceId(resourceID);
            dmr.setRestApiId(apiID);
            FebosAPIGatewayMojoConfigure.apiClient.deleteMethod(dmr);
            System.out.print("[OK]\n");
        } catch (Exception e) {
            System.out.print("[No habia configuración previa]\n");
        }

        try {
            System.out.print("-> Eliminando configuración CORS actual...");
            DeleteMethodRequest dmr = new DeleteMethodRequest();
            dmr.setHttpMethod("OPTIONS");
            dmr.setResourceId(resourceID);
            dmr.setRestApiId(apiID);
            FebosAPIGatewayMojoConfigure.apiClient.deleteMethod(dmr);
            System.out.print("[OK]\n");
        } catch (Exception e) {
            System.out.print("[No habia configuración previa]\n");
        }

        try {
            System.out.print("-> Creando Metodo " + verbo + "... ");
            PutMethodRequest pmr = new PutMethodRequest();
            pmr.setHttpMethod(verbo);
            pmr.setOperationName(lambdaName);
            pmr.setResourceId(resourceID);
            pmr.setRestApiId(apiID);
            pmr.setAuthorizationType("NONE");

            Map<String, Boolean> parametrosR = new HashMap<>();
            String[] arrHeaders = header.split(",");
            for (String head : arrHeaders) {
                parametrosR.put("method.request.header." + head, false);
            }
            pmr.setRequestParameters(parametrosR);

            //pmr.setRequestModels(emptyModel);
            PutMethodResult putMethod = FebosAPIGatewayMojoConfigure.apiClient.putMethod(pmr);
            System.out.print("[OK]\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("[Ya existia el metodo]\n");
        }

        try {
            System.out.print("-> Creando Metodo OPTIONS... ");
            PutMethodRequest pmr = new PutMethodRequest();
            pmr.setHttpMethod("OPTIONS");
            pmr.setOperationName("cors-" + lambdaName);
            pmr.setResourceId(resourceID);
            pmr.setRestApiId(apiID);
            pmr.setAuthorizationType("NONE");

            Map<String, Boolean> parametrosR = new HashMap<>();
            String[] arrHeaders = header.split(",");
            for (String head : arrHeaders) {
                parametrosR.put("method.request.header." + head, false);
            }


            pmr.setRequestParameters(parametrosR);

            //pmr.setRequestModels(emptyModel);
            PutMethodResult putMethod = FebosAPIGatewayMojoConfigure.apiClient.putMethod(pmr);
            System.out.print("[OK]\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("[Ya existia el metodo]\n");
        }

        Map<String, String> velocity = new HashMap<>();
        String tmplJson = "{\n";
        String tmplXml = "{\n";
        Template template1 = null;
        if (mappingFile != null) {
            template1 = new Template(mappingFile).invoke();
        } else {
            template1 = new Template(template).invoke();

        }
        tmplJson = template1.getTmplJson();
        tmplXml = template1.getTmplXml();

        velocity.put("application/json", tmplJson);
        velocity.put("application/xml", tmplXml);

        System.out.print("-> Configurando API para interactuar con el lambda... ");
        PutIntegrationRequest pir = new PutIntegrationRequest();
        pir.setIntegrationHttpMethod("POST");
        pir.setHttpMethod(verbo);
        pir.setPassthroughBehavior("WHEN_NO_TEMPLATES");
        pir.setType(IntegrationType.AWS);
        pir.setResourceId(resourceID);
        pir.setCredentials("");
        pir.setRestApiId(apiID);
        pir.setUri("arn:aws:apigateway:" + region + ":lambda:path/2015-03-31/functions/arn:aws:lambda:" + region + ":" + accountId + ":function:" + lambdaName + ":${stageVariables." + (stageDescriptor != null ? stageDescriptor : "stage") + "}/invocations");
        pir.setRequestTemplates(velocity);

        getLog().info(pir.getUri());
        pir.setContentHandling(ContentHandlingStrategy.CONVERT_TO_TEXT);
        PutIntegrationResult putIntegration = FebosAPIGatewayMojoConfigure.apiClient.putIntegration(pir);
        System.out.print("[OK]\n");

        System.out.print("-> Configurando API para interactuar con el FRONT END... ");
        pir = new PutIntegrationRequest();
        //pir.setIntegrationHttpMethod("POST");
        pir.setHttpMethod("OPTIONS");
        pir.setType(IntegrationType.MOCK);
        pir.setResourceId(resourceID);
        pir.setPassthroughBehavior("WHEN_NO_TEMPLATES");
        pir.setRestApiId(apiID);
        Map<String, String> http200 = new HashMap<>();
        http200.put("application/json", "{\"statusCode\": 200}");
        http200.put("application/xml", "{\"statusCode\": 200}");
        pir.setRequestTemplates(http200);
        putIntegration = FebosAPIGatewayMojoConfigure.apiClient.putIntegration(pir);
        System.out.print("[OK]\n");

        try {
            System.out.print("-> Eliminando actual configuración de Response Method para API... ");
            DeleteMethodResponseRequest dmrr = new DeleteMethodResponseRequest();
            dmrr.setHttpMethod(verbo);
            dmrr.setStatusCode("200");
            dmrr.setResourceId(resourceID);
            dmrr.setRestApiId(apiID);
            FebosAPIGatewayMojoConfigure.apiClient.deleteMethodResponse(dmrr);
            System.out.print("[OK]\n");
        } catch (Exception e) {
            System.out.print("[No existia el metodo http code 200]\n");
        }

        try {
            System.out.print("-> Eliminando actual OPTIONS Response Method para API (CORS)... ");
            DeleteMethodResponseRequest dmrr = new DeleteMethodResponseRequest();
            dmrr.setHttpMethod("OPTIONS");
            dmrr.setStatusCode("200");
            dmrr.setResourceId(resourceID);
            dmrr.setRestApiId(apiID);
            FebosAPIGatewayMojoConfigure.apiClient.deleteMethodResponse(dmrr);
            System.out.print("[OK]\n");
        } catch (Exception e) {
            System.out.print("[No existia el metodo http code 200]\n");
        }

        try {
            System.out.print("-> Creando Reponse con cabeceras de CORS... ");
            PutMethodResponseRequest pmrr = new PutMethodResponseRequest();
            pmrr.setHttpMethod(verbo);
            pmrr.setResourceId(resourceID);
            pmrr.setRestApiId(apiID);
            pmrr.setStatusCode("200");
            pmrr.setResponseModels(emptyModels);
            Map<String, Boolean> h = new HashMap<>();
            h.put("method.response.header.Access-Control-Allow-Headers", true);
            h.put("method.response.header.Access-Control-Allow-Origin", true);
            h.put("method.response.header.Access-Control-Allow-Methods", true);
            pmrr.setResponseParameters(h);
            FebosAPIGatewayMojoConfigure.apiClient.putMethodResponse(pmrr);
            System.out.print("[OK]\n");
        } catch (Exception e) {
            System.out.print("[Ya existia el http code 200]\n");
        }


        try {
            System.out.print("-> Creando Reponse con cabeceras de CORS para metodo OPTIONS... ");
            PutMethodResponseRequest pmrr = new PutMethodResponseRequest();
            pmrr.setHttpMethod("OPTIONS");
            pmrr.setResourceId(resourceID);
            pmrr.setRestApiId(apiID);
            pmrr.setStatusCode("200");
            pmrr.setResponseModels(emptyModels);

            Map<String, Boolean> h = new HashMap<>();
            h.put("method.response.header.Access-Control-Allow-Headers", true);
            h.put("method.response.header.Access-Control-Allow-Origin", true);
            h.put("method.response.header.Access-Control-Allow-Methods", true);
            pmrr.setResponseParameters(h);
            FebosAPIGatewayMojoConfigure.apiClient.putMethodResponse(pmrr);
            System.out.print("[OK]\n");
        } catch (Exception e) {
            System.out.print("[Ya existia el http code 200]\n");
        }

        PutIntegrationResponseRequest pirr = new PutIntegrationResponseRequest();
        Map<String, String> params = new HashMap<>();
        Map<String, String> l = new HashMap<>();

        if (mappingFileResponse != null && mappingFileResponse.exists()) {
            Template templateR = new Template(mappingFileResponse).invoke();
            Map<String, String> velocityR = new HashMap<>();
            velocityR.put("application/json", templateR.getTmplJson());
            velocityR.put("application/xml", "Empty");

            try {
                System.out.print("-> Creando integracion con mapping para CORS... DESDE ARCHIVO");
                pirr.setHttpMethod(verbo);
                pirr.setResourceId(resourceID);
                pirr.setRestApiId(apiID);
                pirr.setStatusCode("200");
                pirr.setResponseTemplates(velocityR);
                params.put("method.response.header.Access-Control-Allow-Headers", "'Accept,Content-Type,X-Amz-Date,Authorization,X-Api-Key," + header + "'");
                params.put("method.response.header.Access-Control-Allow-Origin", "'*'");
                params.put("method.response.header.Access-Control-Allow-Methods", "'GET,PUT,OPTIONS,POST,DELETE,HEAD'");
                pirr.setResponseParameters(params);
                FebosAPIGatewayMojoConfigure.apiClient.putIntegrationResponse(pirr);
                System.out.print("[OK]\n");
            } catch (Exception e) {
                System.out.println("[Ya existia el http code 200]  " + verbo);
            }
        } else {
            System.out.print("-> Creando integracion con mapping para CORS... ");
            pirr.setHttpMethod(verbo);
            pirr.setResourceId(resourceID);
            pirr.setRestApiId(apiID);
            pirr.setStatusCode("200");
            l.put("application/json", "");
            l.put("application/xml", "");
            pirr.setResponseTemplates(l);
            params.put("method.response.header.Access-Control-Allow-Headers", "'Accept,Content-Type,X-Amz-Date,Authorization,X-Api-Key," + header + "'");
            params.put("method.response.header.Access-Control-Allow-Origin", "'*'");
            params.put("method.response.header.Access-Control-Allow-Methods", "'GET,PUT,OPTIONS,POST,DELETE,HEAD'");
            pirr.setResponseParameters(params);
            FebosAPIGatewayMojoConfigure.apiClient.putIntegrationResponse(pirr);
            System.out.print("[OK]\n");
        }

        System.out.print("-> Creando integracion con mapping para CORS (OPTIONS)... ");
        pirr = new PutIntegrationResponseRequest();
        pirr.setHttpMethod("OPTIONS");
        pirr.setResourceId(resourceID);
        pirr.setRestApiId(apiID);
        pirr.setStatusCode("200");
        l = new HashMap<>();
        l.put("application/json", "");
        l.put("application/xml", "");
        pirr.setResponseTemplates(l);
        params = new HashMap<>();
        params.put("method.response.header.Access-Control-Allow-Headers", "'Accept,Content-Type,X-Amz-Date,Authorization,X-Api-Key," + header + "'");
        params.put("method.response.header.Access-Control-Allow-Origin", "'*'");
        params.put("method.response.header.Access-Control-Allow-Methods", "'GET,PUT,OPTIONS,POST,DELETE,HEAD'");
        pirr.setResponseParameters(params);
        FebosAPIGatewayMojoConfigure.apiClient.putIntegrationResponse(pirr);
        System.out.print("[OK]\n");
    }


}
