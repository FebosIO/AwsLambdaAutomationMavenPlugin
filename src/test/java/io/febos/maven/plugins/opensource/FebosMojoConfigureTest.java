package io.febos.maven.plugins.opensource;

import com.google.gson.Gson;
import io.febos.maven.plugins.opensource.FebosLambdaMojoConfigure;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Ignore;
import org.junit.Test;

public class FebosMojoConfigureTest {

    @Test
    @Ignore
    public void ejecutar() throws MojoFailureException, MojoExecutionException {

        String lambdas = "{\"ram\":1024,\"timeout\":900,\"nombre\":\"io_documentos\",\"handler\":\"io.febos.framework.lambda.launchers.LaunchHandler::execute\",\"localFile\":\"/Users/claudio/IdeaProjects/Febos3-Backend/global/lambdav2/io_documentos/target/io_documentos-3.8.7-lambda.jar\",\"s3File\":\"febos-io/builds/v2/lambdas/io_documentos-3.8.7-lambda.jar\",\"descripcion\":\"Agrupa dominio sobre los documentos\",\"role\":\"arn:aws:iam::830321976775:role/lambda-vpc-execution-role\",\"vpc\":\"SubnetIds\\u003dsubnet-f5c17cc9,subnet-ca0a0cc6,subnet-9112989e,subnet-5322ac19,subnet-13bda02c,subnet-2b9c504c,subnet-68e41e34,subnet-b1b7aa8e,SecurityGroupIds\\u003dsg-17b15c6b\",\"stages\":\"slot0,slot1,slot2,slot3,slot5,slot5,slot6,slot7,slot8,slot9,desarrollo,pruebas,certificacion,produccion\",\"warmerHandler\":\"io.febos.framework.lambda.warm.WarmerHandler\",\"warmerRequest\":\"io.febos.framework.lambda.warm.WarmerRequest\",\"warmerResponse\":\"io.febos.framework.lambda.warm.WarmerResponse\",\"layers\":[\"arn:aws:lambda:us-east-1:830321976775:layer:io-core-layer:4\"]}";
        String endponit = "[{\"api\":\"smrvk3euqf\",\"metodo\":\"POST\",\"resource\":\"ha24xo\",\"headers\":\"token,empresa,grupo,describe,ambito,rutCliente,rutProveedor,debug\",\"mapping\":{\"ip\":\"$context.identity.sourceIp\",\"urlRequest\":\"$context.path\",\"stage\":\"$stageVariables.ambiente\",\"token\":\"$input.params(\\u0027token\\u0027)\",\"rutProveedor\":\"$input.params(\\u0027rutProveedor\\u0027)\",\"rutCliente\":\"$input.params(\\u0027rutCliente\\u0027)\",\"empresa\":\"$input.params(\\u0027empresa\\u0027)\",\"functionClass\":\"io.febos.dnt.acciones.emision.NuevoDntHandler\",\"responseClass\":\"io.febos.emision.compartido.NuevoDocumentoResponse\",\"requestClass\":\"io.febos.emision.compartido.NuevoDocumentoRequest\",\"pais\":\"$stageVariables.pais\",\"contenidoArchivoIntegracion\":\"$input.json(\\u0027$.contenidoArchivoIntegracion\\u0027)\",\"retornarJson\":\"$input.params(\\u0027retornarJson\\u0027)\",\"retornarXml\":\"$input.params(\\u0027retornarXml\\u0027)\",\"retornarTimbre\":\"$input.params(\\u0027retornarTimbre\\u0027)\",\"tipoDocumento\":\"$input.params(\\u0027tipoDocumento\\u0027)\",\"origen\":\"$input.params(\\u0027origen\\u0027)\",\"firmar\":\"$input.params(\\u0027firmar\\u0027)\",\"idInterno\":\"$input.params(\\u0027idInterno\\u0027)\",\"entrada\":\"$input.params(\\u0027entrada\\u0027)\",\"foliar\":\"$input.params(\\u0027foliar\\u0027)\"}}]";
        Gson g = new Gson();
        FebosLambdaMojoConfigure h = g.fromJson("{\"lambda\":"+lambdas+",\"endpoints\":"+endponit+"}", FebosLambdaMojoConfigure.class);

        System.out.println(g.toJson(h));
        h.deleteJars=false;
        h.update=true;
        h.region="us-east-1";
        h.accountId="830321976775";
        h.execute();

    }
}