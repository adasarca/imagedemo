package com.adasarca.imagedemo.service;

import com.adasarca.imagedemo.model.exception.AmazonS3Exception;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;

@Service
public class AmazonS3Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Service.class);

    private final AmazonS3 amazonS3Client;

    @Value("${amazon.s3.presignedurl.minutes}")
    private Integer presignedUrlMinutes;

    @Autowired
    public AmazonS3Service(AmazonS3 amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    public void upload(String bucketName, String key, byte[] content, String contentType) throws AmazonS3Exception {
        LOGGER.debug("Uploading file [{}] to S3 bucket [{}]...", key, bucketName);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(content.length);
        InputStream inputStream = new ByteArrayInputStream(content);

        try {
            amazonS3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));
        } catch (RuntimeException exception) {
            throw new AmazonS3Exception(exception);
        }
    }

    public void delete(String bucketName, String key) throws AmazonS3Exception {
        LOGGER.debug("Deleting file [{}] from S3 bucket [{}]...", key, bucketName);

        try {
            amazonS3Client.deleteObject(bucketName, key);
        } catch (RuntimeException exception) {
            throw new AmazonS3Exception(exception);
        }
    }

    public URL generatePreSignedUrl(String bucketName, String key) throws AmazonS3Exception {
        LOGGER.debug("Generating presigned URL for key [{}] and S3 bucket [{}]...", key, bucketName);

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, presignedUrlMinutes);
            return amazonS3Client.generatePresignedUrl(bucketName, key, calendar.getTime());
        } catch (RuntimeException exception) {
            throw new AmazonS3Exception(exception);
        }
    }
}
