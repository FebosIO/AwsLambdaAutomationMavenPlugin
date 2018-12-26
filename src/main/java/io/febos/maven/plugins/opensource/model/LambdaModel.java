package io.febos.maven.plugins.opensource.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.List;
import java.util.Map;

@DynamoDBTable(tableName = "lambdas")
public class LambdaModel {
    private String name;
    private String description;
    private int awsDeployNumber;
    private Map<Integer, List<String>> aliases;

    @DynamoDBHashKey(attributeName = "nombre")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDBAttribute(attributeName = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @DynamoDBAttribute(attributeName = "awsDeployNumber")
    public int getAwsDeployNumber() {
        return awsDeployNumber;
    }

    public void setAwsDeployNumber(int awsDeployNumber) {
        this.awsDeployNumber = awsDeployNumber;
    }

    @DynamoDBAttribute(attributeName = "aliases")
    public Map<Integer, List<String>> getAliases() {
        return aliases;
    }

    public void setAliases(Map<Integer, List<String>> aliases) {
        this.aliases = aliases;
    }
}
