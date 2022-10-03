package com.adasarca.imagedemo.repository;

import com.adasarca.imagedemo.model.database.UniqueFieldRecord;
import com.adasarca.imagedemo.model.database.UserCredentialsRecord;
import com.adasarca.imagedemo.model.enumeration.RepositoryName;
import com.adasarca.imagedemo.model.enumeration.UniqueFieldType;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;

import java.util.*;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Service
public class UserCredentialsRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserCredentialsRepository.class);

    private final DynamoDbEnhancedClient dynamoDbenhancedClient;

    @Autowired
    public UserCredentialsRepository(DynamoDbEnhancedClient dynamoDbenhancedClient) {
        this.dynamoDbenhancedClient = dynamoDbenhancedClient;
    }

    public UserCredentialsRecord findById(String userId) throws DatabaseException {
        LOGGER.debug("Retrieving UserCredentials for userId [{}]...", userId);

        try {
            DynamoDbTable<UserCredentialsRecord> table = getTable();
            Key key = Key.builder().partitionValue(userId).build();
            return table.getItem(key);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public UserCredentialsRecord findByEmail(String email) throws DatabaseException {
        LOGGER.debug("Retrieving UserCredentials by email [{}]...", email);

        try {
            DynamoDbTable<UserCredentialsRecord> table = getTable();
            DynamoDbIndex<UserCredentialsRecord> index = table.index("EmailIndex");

            Iterator<Page<UserCredentialsRecord>> results =
                    index.query(r -> r.queryConditional(keyEqualTo(k -> k.partitionValue(email)))).iterator();

            if (!results.hasNext())
                return null;

            Page<UserCredentialsRecord> page = results.next();
            if (page.items() == null || page.items().isEmpty())
                return null;

            if (page.items().size() > 1) {
                LOGGER.error("Found multiple entries for Email [{}], returning null...", email);
                return null;
            }

            return page.items().get(0);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public void insert(UserCredentialsRecord userCredentialsRecord) throws ValidationException, DatabaseException {
        LOGGER.debug("Inserting UserCredentials to database...");

        if (null == userCredentialsRecord || !StringUtils.hasLength(userCredentialsRecord.getUserId()) ||
                !StringUtils.hasLength(userCredentialsRecord.getEmail()) || !StringUtils.hasLength(userCredentialsRecord.getPassword()))
            throw new ValidationException("Invalid UserCredentials");

        try {
            DynamoDbTable<UserCredentialsRecord> table = getTable();
            DynamoDbTable<UniqueFieldRecord> uniqueTable = getUniqueTable();

            //insert UniqueField
            Expression idConditionExpression = Expression.builder()
                    .expression("attribute_not_exists(UserId)")
                    .build();
            PutItemEnhancedRequest<UserCredentialsRecord> credentialsRequest = PutItemEnhancedRequest.builder(UserCredentialsRecord.class)
                    .item(userCredentialsRecord)
                    .conditionExpression(idConditionExpression)
                    .build();

            //insert UserCredentials
            Expression emailConditionExpression = Expression.builder()
                    .expression("attribute_not_exists(UniqueValue) and attribute_not_exists(UniqueType)")
                    .build();
            PutItemEnhancedRequest<UniqueFieldRecord> uniqueRequest = PutItemEnhancedRequest.builder(UniqueFieldRecord.class)
                    .item(new UniqueFieldRecord(userCredentialsRecord.getEmail(), UniqueFieldType.UserEmail.toString()))
                    .conditionExpression(emailConditionExpression)
                    .build();

            //execute transaction
            TransactWriteItemsEnhancedRequest transactRequest = TransactWriteItemsEnhancedRequest.builder()
                    .addPutItem(uniqueTable, uniqueRequest)
                    .addPutItem(table, credentialsRequest)
                    .build();
            dynamoDbenhancedClient.transactWriteItems(transactRequest);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public void update(UserCredentialsRecord userCredentialsRecord) throws ValidationException, DatabaseException {
        LOGGER.debug("Updating UserCredentials...");

        if (null == userCredentialsRecord || !StringUtils.hasLength(userCredentialsRecord.getUserId()) ||
                !StringUtils.hasLength(userCredentialsRecord.getEmail()) || !StringUtils.hasLength(userCredentialsRecord.getPassword()))
            throw new ValidationException("Invalid UserCredentials");

        try {
            DynamoDbTable<UserCredentialsRecord> table = getTable();
            DynamoDbTable<UniqueFieldRecord> uniqueTable = getUniqueTable();

            //delete UniqueField
            UserCredentialsRecord oldUserCredentialsRecord = this.findById(userCredentialsRecord.getUserId());
            String oldEmail = oldUserCredentialsRecord.getEmail();
            Key deleteKey = Key.builder().partitionValue(oldEmail).sortValue(UniqueFieldType.UserEmail.toString()).build();

            //insert UniqueField
            Expression emailConditionExpression = Expression.builder()
                    .expression("attribute_not_exists(UniqueValue) and attribute_not_exists(UniqueType)")
                    .build();
            PutItemEnhancedRequest<UniqueFieldRecord> uniqueRequest = PutItemEnhancedRequest.builder(UniqueFieldRecord.class)
                    .item(new UniqueFieldRecord(userCredentialsRecord.getEmail(), UniqueFieldType.UserEmail.toString()))
                    .conditionExpression(emailConditionExpression)
                    .build();

            //update UserCredentials
            Expression idConditionExpression = Expression.builder()
                    .expression("attribute_exists(UserId)")
                    .build();
            PutItemEnhancedRequest<UserCredentialsRecord> credentialsRequest = PutItemEnhancedRequest.builder(UserCredentialsRecord.class)
                    .item(userCredentialsRecord)
                    .conditionExpression(idConditionExpression)
                    .build();

            //execute transaction
            TransactWriteItemsEnhancedRequest transactRequest = TransactWriteItemsEnhancedRequest.builder()
                    .addDeleteItem(uniqueTable, deleteKey)
                    .addPutItem(uniqueTable, uniqueRequest)
                    .addPutItem(table, credentialsRequest)
                    .build();
            dynamoDbenhancedClient.transactWriteItems(transactRequest);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public void delete(String userId) {
        LOGGER.debug("Deleting UserCredentials...");

        if (!StringUtils.hasLength(userId)) {
            throw new IllegalArgumentException("userId");
        }

        try {
            DynamoDbTable<UserCredentialsRecord> table = getTable();
            DynamoDbTable<UniqueFieldRecord> uniqueTable = getUniqueTable();

            //delete UniqueField
            UserCredentialsRecord oldUserCredentialsRecord = this.findById(userId);
            String oldEmail = oldUserCredentialsRecord.getEmail();
            Key deleteUniqueFieldKey = Key.builder().partitionValue(oldEmail).sortValue(UniqueFieldType.UserEmail.toString()).build();

            //delete UserCredentials
            Key deleteCredentialsKey = Key.builder().partitionValue(userId).build();

            //execute transaction
            TransactWriteItemsEnhancedRequest transactRequest = TransactWriteItemsEnhancedRequest.builder()
                    .addDeleteItem(uniqueTable, deleteUniqueFieldKey)
                    .addDeleteItem(table, deleteCredentialsKey)
                    .build();
            dynamoDbenhancedClient.transactWriteItems(transactRequest);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    private DynamoDbTable<UserCredentialsRecord> getTable() {
        return dynamoDbenhancedClient.table(RepositoryName.UserCredentials.toString(), TableSchema.fromBean(UserCredentialsRecord.class));
    }

    private DynamoDbTable<UniqueFieldRecord> getUniqueTable() {
        return dynamoDbenhancedClient.table(RepositoryName.UniqueField.toString(), TableSchema.fromBean(UniqueFieldRecord.class));
    }
}
