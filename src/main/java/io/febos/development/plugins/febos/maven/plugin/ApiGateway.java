/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.febos.development.plugins.febos.maven.plugin;

import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.LinkedHashMap;

/**
 * @author Michel M. <michel@febos.cl>
 */
public class ApiGateway {
    private String api;
    private String metodo;
    private String resource;
    private File mappingFile;
    private File mappingFileResponse;
    public String headers="";
    private LinkedHashMap<String, String> mapping;

    /**
     * Retora el valor de la propiedad metodo.
     *
     * @return metodo
     */
    public String metodo() {
        return this.metodo;
    }

    /**
     * Asigna el valor a la propiedad metodo.
     *
     * @param metodo valor a asignar
     */
    public void metodo(String metodo) {
        this.metodo = metodo;
    }

    /**
     * Retora el valor de la propiedad url.
     *
     * @return url
     */
    public String resource() {
        return this.resource;
    }

    /**
     * Asigna el valor a la propiedad url.
     *
     * @param resource valor a asignar
     */
    public void resource(String resource) {
        this.resource = resource;
    }

    /**
     * Retora el valor de la propiedad mapping.
     *
     * @return mapping
     */
    public LinkedHashMap<String, String> mapping() {
        return this.mapping;
    }

    /**
     * Asigna el valor a la propiedad mapping.
     *
     * @param mapping valor a asignar
     */
    public void mapping(LinkedHashMap<String, String> mapping) {
        this.mapping = mapping;
    }

    /**
     * Retora el valor de la propiedad api.
     *
     * @return api
     */
    public String api() {
        return this.api;
    }

    /**
     * Asigna el valor a la propiedad api.
     *
     * @param api valor a asignar
     */
    public void api(String api) {
        this.api = api;
    }


    public File getMappingFile() {
        return mappingFile;
    }

    public File getMappingFileResponse() {
        return mappingFileResponse;
    }

    public void mappingFile(File mappingFile) {
        this.mappingFile = mappingFile;
    }

    public void mappingFileResponse(File mappingFileResponse) {
        this.mappingFileResponse = mappingFileResponse;
    }

}
