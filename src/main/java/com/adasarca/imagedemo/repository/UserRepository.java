package com.adasarca.imagedemo.repository;

import com.adasarca.imagedemo.model.database.UserRecord;
import com.adasarca.imagedemo.model.enumeration.RepositoryName;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.List;

@Service
public class UserRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepository.class);

    private final DynamoDbEnhancedClient dynamoDbenhancedClient;

    @Autowired
    public UserRepository(DynamoDbEnhancedClient dynamoDbenhancedClient) {
        this.dynamoDbenhancedClient = dynamoDbenhancedClient;
    }

    public UserRecord findById(String userId) throws DatabaseException {
        LOGGER.debug("Retrieving User for userId [{}]...", userId);

        try {
            DynamoDbTable<UserRecord> table = getTable();
            Key key = Key.builder().partitionValue(userId).build();
            return table.getItem(key);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public void insert(UserRecord userRecord) throws ValidationException, DatabaseException {
        LOGGER.debug("Inserting User to database...");

        if (null == userRecord || !StringUtils.hasLength(userRecord.getId()) || !StringUtils.hasLength(userRecord.getFirstName()) || !StringUtils.hasLength(userRecord.getLastName())
            || userRecord.getRoleId() == null || userRecord.getCreatedAt() == null)
            throw new ValidationException("Invalid User");

        try {
            DynamoDbTable<UserRecord> table = getTable();

            Expression idConditionExpression = Expression.builder()
                    .expression("attribute_not_exists(UserId)")
                    .build();
            PutItemEnhancedRequest<UserRecord> putRequest = PutItemEnhancedRequest.builder(UserRecord.class)
                    .item(userRecord)
                    .conditionExpression(idConditionExpression)
                    .build();

            table.putItem(putRequest);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public void update(UserRecord userRecord) throws ValidationException, DatabaseException {
        LOGGER.debug("Updating User...");

        if (null == userRecord || !StringUtils.hasLength(userRecord.getId()) || !StringUtils.hasLength(userRecord.getFirstName()) || !StringUtils.hasLength(userRecord.getLastName())
                || userRecord.getRoleId() == null || userRecord.getCreatedAt() == null)
            throw new ValidationException("Invalid User");

        try {
            DynamoDbTable<UserRecord> table = getTable();

            Expression idConditionExpression = Expression.builder()
                    .expression("attribute_exists(UserId)")
                    .build();
            PutItemEnhancedRequest<UserRecord> putRequest = PutItemEnhancedRequest.builder(UserRecord.class)
                    .item(userRecord)
                    .conditionExpression(idConditionExpression)
                    .build();

            table.putItem(putRequest);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    private DynamoDbTable<UserRecord> getTable() {
        return dynamoDbenhancedClient.table(RepositoryName.User.toString(), TableSchema.fromBean(UserRecord.class));
    }
}
