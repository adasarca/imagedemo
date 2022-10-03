package com.adasarca.imagedemo.model.database;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.ZonedDateTime;

@DynamoDbBean
public class PostRecord {

    private String userId;
    private String postId;
    private String imageKey;
    private String description;
    private Long expirationTime;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    @DynamoDbAttribute("UserId")
    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("PostId")
    @DynamoDbSortKey
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    @DynamoDbAttribute("ImageKey")
    @DynamoDbSecondaryPartitionKey(indexNames = "ImageKeyIndex")
    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    @DynamoDbAttribute("Description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @DynamoDbAttribute("ExpirationTime")
    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    @DynamoDbAttribute("CreatedAt")
    @DynamoDbSecondarySortKey(indexNames = "CreatedAtIndex")
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("UpdatedAt")
    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
