package com.adasarca.imagedemo.service;

import com.adasarca.imagedemo.model.exception.AmazonSQSException;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

@Service
public class AmazonSQSListener implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonSQSListener.class);

    private final PostService postService;

    @Autowired
    public AmazonSQSListener(PostService postService) {
        this.postService = postService;
    }

    @Override
    public void onMessage(Message message) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            SQSTextMessage sqsTextMessage = (SQSTextMessage) message;
            S3EventNotification notification = objectMapper.readValue(sqsTextMessage.getText(), S3EventNotification.class);

            for (S3EventNotification.S3EventNotificationRecord record : notification.getRecords()) {
                S3EventNotification.S3Entity s3Entity = record.getS3();
                if (record.getEventNameAsEnum().equals(S3Event.ObjectCreatedByPut)) {
                    LOGGER.info("Received S3 notification [{}] for key [{}] and bucket [{}]...", record.getEventName(), s3Entity.getObject().getKey(), s3Entity.getBucket().getName());
                    this.postService.markPostAsUploaded(s3Entity.getObject().getKey());
                }
            }

        } catch (DatabaseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AmazonSQSException(exception);
        }
    }
}
