/*
 * Copyright (C) IA Solutions Ltda - Todos los derechos reservados
 * Queda expresamente prohibida la copia o reproducción total o parcial de este archivo
 * sin el permiso expreso y por escrito de IA Solutions LTDA. 
 * La detección de un uso no autorizado puede acarrear el inicio de acciones legales.
 */
package io.febos.development.plugins.febos.maven.plugin;

/**
 *
 * @author Michel M. <michel@febos.cl>
 */
public class Lambda {
    private int ram;
    private int timeout;
    private String nombre;
    private String handler;
    private String localFile;
    private String s3File;
    private String descripcion;
    private String role;
    private String vpc;

    /**
     * Retora el valor de la propiedad ram.
     * @return ram
     */
    public int ram() {
        return this.ram;
    }

    /**
     * Asigna el valor a la propiedad ram.
     * @param ram valor a asignar
     */
    public void ram(int ram) {
        this.ram = ram;
    }

    /**
     * Retora el valor de la propiedad timeout.
     * @return timeout
     */
    public int timeout() {
        return this.timeout;
    }

    /**
     * Asigna el valor a la propiedad timeout.
     * @param timeout valor a asignar
     */
    public void timeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Retora el valor de la propiedad nombre.
     * @return nombre
     */
    public String nombre() {
        return this.nombre;
    }

    /**
     * Asigna el valor a la propiedad nombre.
     * @param nombre valor a asignar
     */
    public void nombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Retora el valor de la propiedad localFile.
     * @return localFile
     */
    public String localFile() {
        return this.localFile;
    }

    /**
     * Asigna el valor a la propiedad localFile.
     * @param localFile valor a asignar
     */
    public void localFile(String localFile) {
        this.localFile = localFile;
    }

    /**
     * Retora el valor de la propiedad s3File.
     * @return s3File
     */
    public String s3File() {
        return this.s3File;
    }

    /**
     * Asigna el valor a la propiedad s3File.
     * @param s3File valor a asignar
     */
    public void s3File(String s3File) {
        this.s3File = s3File;
    }

    /**
     * Retora el valor de la propiedad descripcion.
     * @return descripcion
     */
    public String descripcion() {
        return this.descripcion;
    }

    /**
     * Asigna el valor a la propiedad descripcion.
     * @param descripcion valor a asignar
     */
    public void descripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Retora el valor de la propiedad role.
     * @return role
     */
    public String role() {
        return this.role;
    }

    /**
     * Asigna el valor a la propiedad role.
     * @param role valor a asignar
     */
    public void role(String role) {
        this.role = role;
    }

    /**
     * Retora el valor de la propiedad vpc.
     * @return vpc
     */
    public String vpc() {
        return this.vpc;
    }

    /**
     * Asigna el valor a la propiedad vpc.
     * @param vpc valor a asignar
     */
    public void vpc(String vpc) {
        this.vpc = vpc;
    }
    
     /**
     * Retora el valor de la propiedad handler.
     * @return vpc
     */
    public String handler() {
        return this.handler;
    }

    /**
     * Asigna el valor a la propiedad handler.
     * @param handler valor a asignar
     */
    public void handler(String handler) {
        this.handler = handler;
    }
    
    
   
}
