package com.adasarca.imagedemo.repository;

import com.adasarca.imagedemo.model.database.PostRecord;
import com.adasarca.imagedemo.model.database.UserCredentialsRecord;
import com.adasarca.imagedemo.model.enumeration.RepositoryName;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Service
public class PostRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostRepository.class);

    private final DynamoDbEnhancedClient dynamoDbenhancedClient;

    @Autowired
    public PostRepository(DynamoDbEnhancedClient dynamoDbenhancedClient) {
        this.dynamoDbenhancedClient = dynamoDbenhancedClient;
    }

    public PostRecord findByPostId(String userId, String postId) throws DatabaseException {
        LOGGER.debug("Retrieving Post for userId [{}] and postId [{}]...", userId, postId);

        try {
            DynamoDbTable<PostRecord> table = getTable();
            Key key = Key.builder().partitionValue(userId).sortValue(postId).build();
            return table.getItem(key);
        } catch (Exception exception) {
            throw new DatabaseException(exception);
        }
    }

    public List<PostRecord> findAllByUserId(String userId) throws DatabaseException {
        LOGGER.debug("Retrieving Posts for userId [{}]...", userId);

        try {
            DynamoDbTable<PostRecord> table = getTable();
            DynamoDbIndex<PostRecord> index = table.index("CreatedAtIndex");

            Key key = Key.builder().partitionValue(userId).sortValue(ZonedDateTime.now(ZoneId.of("UTC")).toString()).build();
            QueryConditional queryConditional = QueryConditional.sortLessThan(key);
            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder().queryConditional(queryConditional).scanIndexForward(false).build();

            List<PostRecord> list = new LinkedList<>();
            SdkIterable<Page<PostRecord>> results = index.query(queryRequest);
            for (Page<PostRecord> postRecordPage : results) {
                list.addAll(postRecordPage.items());
            }
            return list;
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public List<PostRecord> findCompletedByUserId(String userId) throws DatabaseException {
        LOGGER.debug("Retrieving Posts for userId [{}]...", userId);

        try {
            DynamoDbTable<PostRecord> table = getTable();
            DynamoDbIndex<PostRecord> index = table.index("CreatedAtIndex");

            Key key = Key.builder().partitionValue(userId).sortValue(ZonedDateTime.now(ZoneId.of("UTC")).toString()).build();
            Expression expression = Expression.builder()
                    .expression("attribute_not_exists(ExpirationTime)")
                    .build();

            QueryConditional queryConditional = QueryConditional.sortLessThan(key);
            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .filterExpression(expression)
                    .scanIndexForward(false)
                    .build();

            List<PostRecord> list = new LinkedList<>();
            SdkIterable<Page<PostRecord>> results = index.query(queryRequest);
            for (Page<PostRecord> postRecordPage : results) {
                list.addAll(postRecordPage.items());
            }
            return list;
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public PostRecord findByImageKey(String imageKey) throws DatabaseException {
        LOGGER.debug("Retrieving Post by imageKey [{}]...", imageKey);

        try {
            DynamoDbTable<PostRecord> table = getTable();
            DynamoDbIndex<PostRecord> index = table.index("ImageKeyIndex");

            Iterator<Page<PostRecord>> results =
                    index.query(r -> r.queryConditional(keyEqualTo(k -> k.partitionValue(imageKey)))).iterator();

            if (!results.hasNext())
                return null;

            Page<PostRecord> page = results.next();
            if (page.items() == null || page.items().isEmpty())
                return null;

            if (page.items().size() > 1) {
                LOGGER.error("Found multiple entries for ImageKey [{}], returning null...", imageKey);
                return null;
            }

            return page.items().get(0);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public void save(PostRecord record) throws ValidationException, DatabaseException {
        LOGGER.debug("Saving Post...");

        if (null == record || !StringUtils.hasLength(record.getUserId()) || !StringUtils.hasLength(record.getPostId()) ||
            null == record.getCreatedAt() || null == record.getUpdatedAt())
            throw new ValidationException("Invalid PostRecord");

        try {
            DynamoDbTable<PostRecord> table = getTable();
            table.putItem(record);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    public void delete(String userId, String postId) throws ValidationException, DatabaseException {
        LOGGER.debug("Deleting Post for userId [{}] and postId [{}]...", userId, postId);

        try {
            DynamoDbTable<PostRecord> table = getTable();
            Key key = Key.builder().partitionValue(userId).sortValue(postId).build();
            table.deleteItem(key);
        } catch (RuntimeException exception) {
            throw new DatabaseException(exception);
        }
    }

    private DynamoDbTable<PostRecord> getTable() {
        return dynamoDbenhancedClient.table(RepositoryName.Post.toString(), TableSchema.fromBean(PostRecord.class));
    }
}
