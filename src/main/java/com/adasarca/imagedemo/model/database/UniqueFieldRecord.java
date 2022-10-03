package com.adasarca.imagedemo.model.database;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class UniqueFieldRecord {

    private String uniqueValue;
    private String uniqueType;

    public UniqueFieldRecord() { }

    public UniqueFieldRecord(String uniqueValue, String uniqueType) {
        this.uniqueValue = uniqueValue;
        this.uniqueType = uniqueType;
    }

    @DynamoDbAttribute("UniqueValue")
    @DynamoDbPartitionKey
    public String getUniqueValue() {
        return uniqueValue;
    }

    public void setUniqueValue(String uniqueValue) {
        this.uniqueValue = uniqueValue;
    }

    @DynamoDbAttribute("UniqueType")
    @DynamoDbSortKey
    public String getUniqueType() {
        return uniqueType;
    }

    public void setUniqueType(String uniqueType) {
        this.uniqueType = uniqueType;
    }
}
