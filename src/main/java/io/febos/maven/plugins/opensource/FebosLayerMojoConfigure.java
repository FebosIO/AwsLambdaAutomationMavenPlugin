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
import com.amazonaws.services.apigateway.model.Method;
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
@Mojo(name = "layer",requiresProject = true,requiresDirectInvocation = true)
public class FebosLayerMojoConfigure extends AbstractMojo {

    @Parameter(defaultValue = "true")
    boolean update;
    @Parameter
    boolean deleteJars;
    @Parameter(defaultValue = "")
    String credencialesAWS;
    @Parameter
    List<ApiGateway> endpoints;
    @Parameter
    Layer layer;
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
        lambdaClient = AWSLambdaClientBuilder.standard().withRegion(region).build();
        s3client=AmazonS3ClientBuilder.standard().withRegion(region).build();
        subirArchivoEnS3(layer.localFile,layer.s3File);
        getLog().info("Configurando Layer");
        PublishLayerVersionRequest lv=new PublishLayerVersionRequest();
        List<String> runtimes=new ArrayList<>();
        runtimes.add("java8");
        lv.setCompatibleRuntimes(runtimes);
        LayerVersionContentInput contentLayer=new LayerVersionContentInput();
        String bucketLayer = layer.s3File.split("/")[0];
        String s3pathLayer = layer.s3File.substring(layer.s3File.indexOf("/") + 1);
        contentLayer.setS3Key(s3pathLayer);
        contentLayer.setS3Bucket(bucketLayer);
        lv.setContent(contentLayer);
        lv.setLayerName(layer.nombre);
        lv.setDescription(layer.descripcion);
        lv.setLicenseInfo(layer.licencia);
        lambdaClient.publishLayerVersion(lv);

    }
    private void subirArchivoEnS3(String localFile, String s3File) {
        getLog().info("Subiendo package a S3 (" + (new File(localFile).length() / 1000000) + " MB)");
        getLog().info(localFile);
        String bucket = s3File.split("/")[0];
        String s3path = s3File.substring(s3File.indexOf("/") + 1);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, s3path, new File(localFile));
        final long fileSize = new File(localFile).length();
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
    }


}
