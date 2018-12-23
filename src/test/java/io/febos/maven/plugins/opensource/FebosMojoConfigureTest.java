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

        String lambdas = "{\"ram\":1024,\"timeout\":120,\"nombre\":\"caf\",\"handler\":\"io.febos.framework.lambda.launchers.LaunchHandle::ejecutar\",\"localFile\":\"/Users/claudio/IdeaProjects/Febos3-Backend/chile/lambdasv2/cl_dte_caf/target/caf-1.0-SNAPSHOT-lambda.jar\",\"s3File\":\"febos-io/builds/v2/lambdas/caf-1.0-SNAPSHOT-lambda.jar\",\"descripcion\":\"Administracion de caf\",\"role\":\"arn:aws:iam::830321976775:role/lambda-vpc-execution-role\",\"vpc\":\"SubnetIds\\u003dsubnet-f5c17cc9,subnet-ca0a0cc6,subnet-9112989e,subnet-5322ac19,subnet-13bda02c,subnet-2b9c504c,subnet-68e41e34,subnet-b1b7aa8e,SecurityGroupIds\\u003dsg-17b15c6b\"}";
        String endponit = "[{\"api\":\"smrvk3euqf\",\"metodo\":\"POST\",\"region\":\"us-east-1\",\"resource\":\"xnhwe5byk7\",\"mapping\":{\"ip\":\"$context.identity.sourceIp\",\"stage\":\"$stageVariables.ambiente\",\"pais\":\"cl\",\"token\":\"$input.params(\\u0027token\\u0027)\",\"empresa\":\"$input.params(\\u0027empresa\\u0027)\",\"functionClass\":\"cl.febos.dte.caf.cargar.FuncionCargarCaf\",\"requestClass\":\"cl.febos.dte.caf.cargar.SolicitudCargarCaf\",\"responseClass\":\"cl.febos.dte.caf.cargar.RespuestaCargarCaf\",\"hello\":\"$input.json(\\u0027$.hello\\u0027)\"}}]";
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