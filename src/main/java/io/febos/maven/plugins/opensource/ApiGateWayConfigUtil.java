/*
 * Copyright (C) IA Solutions Ltda - Todos los derechos reservados
 * Queda expresamente prohibida la copia o reproducción total o parcial de este archivo
 * sin el permiso expreso y por escrito de IA Solutions LTDA. 
 * La detección de un uso no autorizado puede acarrear el inicio de acciones legales.
 */
package io.febos.maven.plugins.opensource;

import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.*;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.annotation.Annotation;


/**
 *
 * @author Michel M. <michel@febos.cl>
 */
public class ApiGateWayConfigUtil {


    public static void configurarApiGateway() {

    }

    public static void generarMapping(Class clazz) {
        Map<String, Object> mapping = new LinkedHashMap();
        mapping.put("stage", "$stageVariables.stage");
        mapping.put("token", "$input.params('token')");
        mapping.put("company", "$input.params('company')");
        mapping.put("ip", "$context.identity.sourceIp");
        System.out.println("Header Params:");
        for (String h : new String[]{"token", "company"}) {
            System.out.println("- " + h);
        }
        System.out.println("\nQuery Params:");
        for (Field propiedad : clazz.getDeclaredFields()) {
            //propiedad.setAccessible(true);
            Annotation[] anotaciones = propiedad.getAnnotations();
            boolean esJson=false;
            for(Annotation anot:anotaciones){
                if(anot.getClass().getName().contains("JsonParameter"))esJson=true;
            }
            if (esJson) {
                mapping.put(propiedad.getName(), "$input.json('$." + propiedad.getName() + "')");
            } else {
                System.out.println("- " + propiedad.getName());
                mapping.put(propiedad.getName(), "$input.params('" + propiedad.getName() + "')");
            }

        }

        String msg;
        try {
            msg = new ObjectMapper().writeValueAsString(mapping);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(msg);
            String prettyJsonString = gson.toJson(je).replaceAll("\\\\u0027", "'");
            String[] lineas=prettyJsonString.split("\n");
            prettyJsonString="";
            for(String linea:lineas){
                if(linea.contains("input.json")){
                    String[] tmp=linea.split(":");
                    prettyJsonString+=tmp[0]+": "+tmp[1].replaceAll("\"","");
                }else{
                   prettyJsonString+=linea; 
                }
                prettyJsonString+="\n";
            }
            
            System.out.println("***** MAPPING *****");
            System.out.println("  application/json");
            System.out.println("*******************");
            System.out.println(prettyJsonString);
            System.out.println("********************");
        } catch (JsonProcessingException ex) {
            System.out.println("Imposible generar mapping");
        }
    }



}
