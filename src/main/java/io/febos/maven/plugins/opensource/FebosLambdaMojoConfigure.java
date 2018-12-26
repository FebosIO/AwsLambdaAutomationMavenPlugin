/*
 * Copyright (C) IA Solutions Ltda - Todos los derechos reservados
 * Queda expresamente prohibida la copia o reproducción total o parcial de este archivo
 * sin el permiso expreso y por escrito de IA Solutions LTDA.
 * La detección de un uso no autorizado puede acarrear el inicio de acciones legales.
 */
package io.febos.maven.plugins.opensource;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.*;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.Gson;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Maven Plugin para facilitar la configuración de API Gateway y Lambda. Permite
 * configurar el lmabda y api dateway desde el POM del proyecto maven.
 * @author Michel M. <michel@febos.cl>
 */
@Mojo(name = "lambda",requiresProject = true,requiresDirectInvocation = true)
public class FebosLambdaMojoConfigure extends AbstractMojo {

    @Parameter(defaultValue = "true")
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

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;

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
        Map<Integer, List<String>> aliases=new HashMap<>();
        try {
            getLog().info("CONFIGURAMOS ");
            Gson g = new Gson();
            getLog().info("REGION  " + region);
            getLog().info("CUENTA  " + accountId);
            getLog().info("LAMBDA " + g.toJson(lambda));
            getLog().info("Endponts " + g.toJson(endpoints));
        } catch (Exception e) {

        }
        getLog().info("API ");
        if (!update) {
            getLog().info("Upload desactivado");
            return;
        }
        lambdaNuevo = false;
        getLog().info("Bucando credenciales: " + credencialesAWS);
        try {
            lambdaClient = AWSLambdaClientBuilder.standard().withRegion(region).build();
            s3client = AmazonS3ClientBuilder.standard().withRegion(region).build();


            getLog().info("Subiendo package a S3 (" + (new File(lambda.localFile()).length() / 1000000) + " MB)");
            getLog().info(lambda.localFile());
            String bucket = lambda.s3File().split("/")[0];
            String s3path = lambda.s3File().substring(lambda.s3File().indexOf("/") + 1);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, s3path, new File(lambda.localFile()));
            final long fileSize = new File(lambda.localFile()).length();
            System.out.println("");
            putObjectRequest.setGeneralProgressListener(new ProgressListener() {
                long bytesSubidos = 0;
                HashMap<Integer, Boolean> avance = new HashMap<>();

                @Override
                public void progressChanged(ProgressEvent pe) {
                    bytesSubidos += pe.getBytesTransferred();
                    int porcentaje = (int) (bytesSubidos * 100 / fileSize);
                    if (!avance.getOrDefault(porcentaje, false)) {
                        avance.put(porcentaje, true);
                        if (porcentaje % 2 == 0) {
                            String bar = "[";
                            for (int i = 2; i <= 100; i = i + 2) {
                                if (i <= porcentaje && porcentaje != 0) {
                                    bar += "#";
                                } else {
                                    bar += " ";
                                }
                            }
                            bar += "] " + porcentaje + "%";
                            System.out.print("\r" + bar);
                            if (porcentaje == 100) {
                                System.out.println("\nListo!");
                            }
                        }
                    }

                }
            });
            s3client.putObject(putObjectRequest);
            getLog().info("--> [OK]");

            //leyendo properties para establecer permisos
            String ruta = new File(lambda.localFile()).getParentFile().getAbsolutePath();
            ruta += ruta.endsWith("/") ? "" : "/";
            ruta += "classes/maven.properties";
            Properties prop = new Properties();
            InputStream input = null;
            try {
                getLog().info("CARGANDO PROPIEDADES " + ruta);

                input = new FileInputStream(ruta);
                // load a properties file
                prop.load(input);

            } catch (IOException ex) {
                //ex.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            }
            //

            if (!functionExists(lambda.nombre())) {
                lambdaNuevo = true;
                getLog().info("Creando funcion  lambda " + lambda.nombre());
                try {
                    CreateFunctionRequest nuevoLambda = new CreateFunctionRequest();
                    VpcConfig vpcConfig = null;
                    if (lambda.vpc() != null) {
                        vpcConfig = new VpcConfig();

                        String[] securityGroups = lambda.vpc().split("SecurityGroupIds")[1].replace("=", "").split(",");
                        String[] subnets = lambda.vpc().split(",SecurityGroupIds")[0].replace("SubnetIds=", "").split(",");
                        //vpcConfig.withSecurityGroupIds(lambda.vpc().split(",")[1].split("=")[1]);
                        //vpcConfig.withSubnetIds(lambda.vpc().split(",")[0].split("=")[1]);
                        vpcConfig.withSecurityGroupIds(securityGroups);
                        vpcConfig.withSubnetIds(subnets);
                    }

                    nuevoLambda
                            .withFunctionName(lambda.nombre())
                            .withDescription("[v."+project.getVersion()+"]"+lambda.descripcion())
                            .withPublish(true)
                            .withHandler(lambda.handler())
                            .withMemorySize(lambda.ram())
                            .withTimeout(lambda.timeout())
                            .withRuntime("java8")
                            .withCode(new FunctionCode().withS3Bucket(bucket).withS3Key(s3path));
                    try {
                        getLog().info("Seteando variables de entorno para el lambda " + lambda.nombre());
                        nuevoLambda.setEnvironment(new Environment());
                        for (Map.Entry<Object, Object> set : prop.entrySet()) {
                            if (((String) set.getKey()).startsWith("febos")) {
                                String key = (String) set.getKey();
                                key = key.replaceAll("\\.", "_");
                                String value = (String) set.getValue();
                                getLog().info("  -> " + key + " = " + value);
                                nuevoLambda.getEnvironment().addVariablesEntry(key, value);
                            }
                        }
                        getLog().info("--> [OK]");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (lambda.role() != null && !lambda.role().isEmpty()) {
                        nuevoLambda.withRole(lambda.role());
                    }
                    if (vpcConfig != null) {
                        nuevoLambda.withVpcConfig(vpcConfig);
                    }
                    CreateFunctionResult createFunction = lambdaClient.createFunction(nuevoLambda);
                    System.out.println(new Gson().toJson(createFunction));
                    getLog().info("--> [OK]");
                    String[] ambientes = lambda.stages != null ? lambda.stages.split(",") : "".split(",");
                    getLog().info("Creando alias para los distintos ambientes");
                    for (String ambiente : ambientes) {
                        getLog().info("--> Configurando " + ambiente);
                        CreateAliasResult createAlias = lambdaClient.createAlias(new CreateAliasRequest()
                                .withFunctionName(lambda.nombre())
                                .withName(ambiente)
                                .withFunctionVersion("$LATEST")
                        );
                        System.out.println(createAlias.getName() + " : " + createAlias.getDescription());
                        lambdaClient.addPermission(new AddPermissionRequest()
                                .withFunctionName("arn:aws:lambda:"
                                        + region //credenciales.props.getProperty("region")
                                        + ":" + accountId //credenciales.props.getProperty("id")
                                        + ":function:" + lambda.nombre() + ":" + ambiente)
                                .withSourceArn("arn:aws:execute-api:" + region + ":" + accountId + ":*")
                                .withPrincipal("apigateway.amazonaws.com")
                                .withStatementId(UUID.randomUUID().toString())
                                .withAction("lambda:InvokeFunction")
                        );
                    }
                    getLog().info("--> [OK]");
                    getLog().info("--> Creando Alias de versión");
                    lambdaClient.createAlias(new CreateAliasRequest()
                            .withFunctionName(lambda.nombre())
                            .withName("v"+project.getVersion().replaceAll("\\.","_"))
                            .withFunctionVersion("$LATEST")
                    );
                    getLog().info("--> [OK]");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                getLog().info("Actualizando codigo lambda " + lambda.nombre());
                UpdateFunctionCodeRequest updateLambda = new UpdateFunctionCodeRequest();
                updateLambda.withFunctionName(lambda.nombre())
                        .withPublish(true)
                        .withS3Bucket(bucket)
                        .withS3Key(s3path);

                lambdaClient.updateFunctionCode(updateLambda);
                getLog().info("--> [OK]");
                getLog().info("Actualizando configuracion");
                UpdateFunctionConfigurationRequest configureLambda = new UpdateFunctionConfigurationRequest();
                VpcConfig vpcConfig = null;

                if (lambda.vpc() != null) {
                    vpcConfig = new VpcConfig();
                    String[] securityGroups = lambda.vpc().split("SecurityGroupIds")[1].replace("=", "").split(",");
                    String[] subnets = lambda.vpc().split(",SecurityGroupIds")[0].replace("SubnetIds=", "").split(",");
                    //vpcConfig.withSecurityGroupIds(lambda.vpc().split(",")[1].split("=")[1]);
                    //vpcConfig.withSubnetIds(lambda.vpc().split(",")[0].split("=")[1]);
                    vpcConfig.withSecurityGroupIds(securityGroups);
                    vpcConfig.withSubnetIds(subnets);

                }

                configureLambda
                        .withFunctionName(lambda.nombre())
                        .withDescription("[v."+project.getVersion()+"]"+lambda.descripcion())
                        .withHandler(lambda.handler())
                        .withMemorySize(lambda.ram())
                        .withTimeout(lambda.timeout())
                        .withRuntime("java8");

                try {
                    getLog().info("Seteando variables de entorno para el lambda");
                    configureLambda.setEnvironment(new Environment());

                    for (Map.Entry<Object, Object> set : prop.entrySet()) {
                        if (((String) set.getKey()).startsWith("proxime")) {
                            String key = (String) set.getKey();
                            key = key.replaceAll("\\.", "_");
                            String value = (String) set.getValue();
                            getLog().info("  -> " + key + " = " + value);
                            configureLambda.getEnvironment().addVariablesEntry(key, value);
                        }
                    }
                    getLog().info("--> [OK]");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (lambda.role() != null && !lambda.role().isEmpty()) {
                    configureLambda.withRole(lambda.role());
                }
                if (vpcConfig != null) {
                    configureLambda.withVpcConfig(vpcConfig);
                }
                lambdaClient.updateFunctionConfiguration(configureLambda);
                getLog().info("--> [OK]");
            }

            HashMap<Integer, String> versiones = new HashMap<>();
            getLog().info("Eliminando versiones sin uso");
            ListVersionsByFunctionRequest reqListVersiones = new ListVersionsByFunctionRequest();
            reqListVersiones.setFunctionName(lambda.nombre());
            ListVersionsByFunctionResult listVersionsByFunction = lambdaClient.listVersionsByFunction(reqListVersiones);
            int maxVersion = 0;
            for (FunctionConfiguration conf : listVersionsByFunction.getVersions()) {
                if (!conf.getVersion().equals("$LATEST")) {
                    int ver = Integer.parseInt(conf.getVersion());
                    versiones.put(ver, null);
                    if (ver > maxVersion) {
                        maxVersion = ver;
                    }
                }
            }
            getLog().info("obteniendo alias");
            ListAliasesRequest reqListAlias = new ListAliasesRequest();
            reqListAlias.setFunctionName(lambda.nombre());
            ListAliasesResult listAliases = lambdaClient.listAliases(reqListAlias);
            for (AliasConfiguration alias : listAliases.getAliases()) {
                if(alias.getName().equalsIgnoreCase("v"+project.getVersion().replaceAll("\\.","_"))){
                    getLog().info(" ** Ya existia un alias para esta versión del lambda, se reemplaza alias por nueva versión");
                    DeleteAliasRequest eliminarAliasReq=new DeleteAliasRequest().withFunctionName(lambda.nombre()).withName("v"+project.getVersion().replaceAll("\\.","_"));
                    lambdaClient.deleteAlias(eliminarAliasReq);
                }
                if (!alias.getFunctionVersion().equals("$LATEST")) {
                    versiones.put(Integer.parseInt(alias.getFunctionVersion().trim()), alias.getName());
                }
            }
            getLog().info("max version " + maxVersion);
            final int maxVer = maxVersion;
            versiones.entrySet().stream().forEach((v) -> {
                if (v.getValue() == null && v.getKey() != maxVer) {
                    try {
                        DeleteFunctionRequest delReq = new DeleteFunctionRequest();
                        delReq.setFunctionName(lambda.nombre());
                        delReq.setQualifier(v.getKey().toString());
                        getLog().info("Eliminando version " + v.getKey() + " (no tiene alias)");
                        lambdaClient.deleteFunction(delReq);

                    } catch (Exception e) {

                    }
                } else {
                    String alias = v.getValue() == null ? "ultima version" : "alias " + v.getValue();
                    getLog().info("Conservando version " + v.getKey() + " ( " + alias + " )");

                    lambdaClient.createAlias(new CreateAliasRequest()
                            .withFunctionName(lambda.nombre())
                            .withName("v"+project.getVersion().replaceAll("\\.","_"))
                            .withFunctionVersion(Integer.toString(maxVer))
                    );


                }
            });
            getLog().info("borrar jar " + deleteJars);
            if (deleteJars) {
                File[] archivos = new File(lambda.localFile()).getParentFile().listFiles();
                for (File archivo : archivos) {
                    if (archivo.isFile()) {
                        archivo.delete();
                    }
                }
            }

            //TODO: save info to Dynamo lambdas, versions and alias


            getLog().info("Precalentando Lambda...");
            AWSLambda cliente = AWSLambdaClientBuilder.standard().withRegion(region).build();
            InvokeRequest invokeRequest = new InvokeRequest();
            invokeRequest.setFunctionName(lambda.nombre());
            String stageName = lambda.stages != null ? lambda.stages.split(",")[0] : "";
            if (!stageName.isEmpty()) {
                invokeRequest.setQualifier(stageName);
            }
            invokeRequest.setPayload("{\"stage\":\"" + stageName + "\",\"warmup\":\"yes\"}");
            InvokeResult invoke = cliente.invoke(invokeRequest);
            getLog().info(new String(invoke.getPayload().array()));


        } catch (Exception e) {
            e.printStackTrace();
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

    public boolean functionExists(String name) {
        try {
            GetFunctionRequest gfr = new GetFunctionRequest();
            gfr.setFunctionName(name);
            GetFunctionResult function = lambdaClient.getFunction(gfr);
            if (function != null) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public void configurarApiGateway(String apiID, String resourceID, String verbo, String lambdaName, Map<String, String> template, File mappingFile, File mappingFileResponse, String header, String region, String accountId) {
        if (apiID == null || apiID.isEmpty()) {
            return;
        }
        FebosLambdaMojoConfigure.apiClient = AmazonApiGatewayClientBuilder.standard().withRegion(region).build();
        FebosLambdaMojoConfigure.lambdaClient = AWSLambdaClientBuilder.standard().withRegion(region).build();

        Map<String, String> emptyModels = new HashMap<>();

        emptyModels.put("application/json", "Empty");
        emptyModels.put("application/xml", "Empty");
        try {
            System.out.print("-> Eliminando configuración actual...");
            DeleteMethodRequest dmr = new DeleteMethodRequest();
            dmr.setHttpMethod(verbo);
            dmr.setResourceId(resourceID);
            dmr.setRestApiId(apiID);
            FebosLambdaMojoConfigure.apiClient.deleteMethod(dmr);
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
            FebosLambdaMojoConfigure.apiClient.deleteMethod(dmr);
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
            PutMethodResult putMethod = FebosLambdaMojoConfigure.apiClient.putMethod(pmr);
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
            PutMethodResult putMethod = FebosLambdaMojoConfigure.apiClient.putMethod(pmr);
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
        PutIntegrationResult putIntegration = FebosLambdaMojoConfigure.apiClient.putIntegration(pir);
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
        putIntegration = FebosLambdaMojoConfigure.apiClient.putIntegration(pir);
        System.out.print("[OK]\n");

        try {
            System.out.print("-> Eliminando actual configuración de Response Method para API... ");
            DeleteMethodResponseRequest dmrr = new DeleteMethodResponseRequest();
            dmrr.setHttpMethod(verbo);
            dmrr.setStatusCode("200");
            dmrr.setResourceId(resourceID);
            dmrr.setRestApiId(apiID);
            FebosLambdaMojoConfigure.apiClient.deleteMethodResponse(dmrr);
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
            FebosLambdaMojoConfigure.apiClient.deleteMethodResponse(dmrr);
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
            FebosLambdaMojoConfigure.apiClient.putMethodResponse(pmrr);
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
            FebosLambdaMojoConfigure.apiClient.putMethodResponse(pmrr);
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
                FebosLambdaMojoConfigure.apiClient.putIntegrationResponse(pirr);
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
            FebosLambdaMojoConfigure.apiClient.putIntegrationResponse(pirr);
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
        FebosLambdaMojoConfigure.apiClient.putIntegrationResponse(pirr);
        System.out.print("[OK]\n");
    }


}
