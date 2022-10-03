package com.adasarca.imagedemo.service;

import com.adasarca.imagedemo.model.database.PostRecord;
import com.adasarca.imagedemo.model.domain.ContextUser;
import com.adasarca.imagedemo.model.domain.Post;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.AmazonS3Exception;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class PostService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostService.class);

    @Value("${amazon.s3.url}")
    private String amazonS3Url;
    @Value("${amazon.s3.postbucket}")
    private String amazonS3Bucket;

    @Value("${posts.description.maxlength}")
    private Integer descriptionMaxLength;
    @Value("${posts.image.contenttypes}")
    private List<String> allowedContentTypes;

    @Value("${posts.ttl.minutes}")
    private Integer postTTL;

    private final PostRepository postRepository;
    private final AmazonS3Service amazonS3Service;

    @Autowired
    public PostService(PostRepository postRepository, AmazonS3Service amazonS3Service) {
        this.postRepository = postRepository;
        this.amazonS3Service = amazonS3Service;
    }

    public List<Post> findAllByUser(ContextUser currentUser) throws DatabaseException {
        if (null == currentUser)
            throw new IllegalArgumentException("CurrentUser is null");

        List<PostRecord> records = postRepository.findAllByUserId(currentUser.getUserId());
        return this.buildPosts(records);
    }

    public List<Post> findCompletedByUser(ContextUser currentUser) throws DatabaseException {
        if (null == currentUser)
            throw new IllegalArgumentException("CurrentUser is null");

        List<PostRecord> records = postRepository.findCompletedByUserId(currentUser.getUserId());
        return this.buildPosts(records);
    }

    public Post upload(ContextUser currentUser, String description, MultipartFile imageFile) throws ValidationException, AmazonS3Exception, DatabaseException {
        if (null == currentUser)
            throw new IllegalArgumentException("CurrentUser is null");

        if (null == imageFile)
            throw new ValidationException("Cannot upload post without an image");

        if (description != null && description.length() > descriptionMaxLength)
            throw new ValidationException("Description exceeds character limit of " + descriptionMaxLength);

        if (!allowedContentTypes.contains(imageFile.getContentType()))
            throw new ValidationException("Invalid image content type");

        String postId = UUID.randomUUID().toString();
        ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("UTC"));

        //build image key
        int index = imageFile.getOriginalFilename() != null ? imageFile.getOriginalFilename().lastIndexOf('.') : -1;
        String extension = index > -1 ? imageFile.getOriginalFilename().substring(index) : "";
        String imageName = postId + extension;
        String imageKey = String.format("%s/%d/%d/%s", currentUser.getUserId(), createdAt.getYear(), createdAt.getMonthValue(), imageName);

        //upload image to AWS S3
        try {
            byte[] content = imageFile.getBytes();
            amazonS3Service.upload(amazonS3Bucket, imageKey, content, imageFile.getContentType());
        } catch (IOException exception) {
            LOGGER.error("Exception reading bytes from uploaded file: ", exception);
            throw new ValidationException("Invalid file");
        }

        //build Post object
        PostRecord record = new PostRecord();
        record.setUserId(currentUser.getUserId());
        record.setPostId(postId);
        record.setDescription(description);
        record.setImageKey(imageKey);
        record.setCreatedAt(createdAt);
        record.setUpdatedAt(createdAt);

        //save Post to database
        try {
            postRepository.save(record);
        } catch (Exception exception) {
            amazonS3Service.delete(amazonS3Bucket, imageKey);
            throw exception;
        }

        return buildPost(record);
    }

    public String create(ContextUser currentUser, String description) throws ValidationException, AmazonS3Exception, DatabaseException {
        if (null == currentUser)
            throw new IllegalArgumentException("CurrentUser is null");

        if (description != null && description.length() > descriptionMaxLength)
            throw new ValidationException("Description exceeds character limit of " + descriptionMaxLength);

        String postId = UUID.randomUUID().toString();
        ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("UTC"));

        //build image key
        String imageKey = String.format("%s/%d/%d/%s", currentUser.getUserId(), createdAt.getYear(), createdAt.getMonthValue(), postId);

        //generate presigned URL for AWS S3
        URL presignedUrl = amazonS3Service.generatePreSignedUrl(amazonS3Bucket, imageKey);

        //time to live
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, postTTL);
        long ttl = calendar.getTimeInMillis();

        //build Post object
        PostRecord record = new PostRecord();
        record.setUserId(currentUser.getUserId());
        record.setPostId(postId);
        record.setDescription(description);
        record.setImageKey(imageKey);
        record.setExpirationTime(ttl);
        record.setCreatedAt(createdAt);
        record.setUpdatedAt(createdAt);

        //save Post to database
        try {
            postRepository.save(record);
        } catch (Exception exception) {
            amazonS3Service.delete(amazonS3Bucket, imageKey);
            throw exception;
        }

        return presignedUrl.toString();
    }

    public Post update(ContextUser currentUser, String postId, String description) throws ValidationException, DatabaseException {
        if (null == currentUser)
            throw new IllegalArgumentException("CurrentUser is null");

        if (description != null && description.length() > descriptionMaxLength)
            throw new ValidationException("Description exceeds character limit of " + descriptionMaxLength);

        PostRecord postRecord = this.postRepository.findByPostId(currentUser.getUserId(), postId);
        if (null == postRecord)
            throw new ValidationException("Invalid post ID");

        postRecord.setDescription(description);
        this.postRepository.save(postRecord);
        return this.buildPost(postRecord);
    }

    public void markPostAsUploaded(String imageKey) throws DatabaseException {
        if (null == imageKey)
            throw new IllegalArgumentException("ImageKey is null");

        PostRecord record = postRepository.findByImageKey(imageKey);
        if (null == record) {
            LOGGER.error("Post not found by imageKey [{}], cannot mark it as uploaded", imageKey);
            return;
        }

        if (record.getExpirationTime() == null) {
            LOGGER.debug("Post with imageKey [{}] is already marked as uploaded, skipping it...", imageKey);
            return;
        }

        record.setExpirationTime(null);
        postRepository.save(record);
    }

    public void delete(ContextUser currentUser, String postId) throws ValidationException, AmazonS3Exception, DatabaseException {
        if (null == currentUser)
            throw new IllegalArgumentException("CurrentUser is null");

        PostRecord postRecord = this.postRepository.findByPostId(currentUser.getUserId(), postId);
        if (null == postRecord)
            throw new ValidationException("Invalid post ID");

        this.postRepository.delete(currentUser.getUserId(), postId);
        amazonS3Service.delete(amazonS3Bucket, postRecord.getImageKey());
    }

    private List<Post> buildPosts(List<PostRecord> records) {
        if (null == records)
            return null;

        List<Post> posts = new LinkedList<>();
        for (PostRecord record : records)
            posts.add(buildPost(record));
        return posts;
    }

    private Post buildPost(PostRecord record) {
        if (null == record)
            return null;

        Post post = new Post();
        post.setUserId(record.getUserId());
        post.setPostId(record.getPostId());
        post.setDescription(record.getDescription());
        if (record.getImageKey() != null) {
            post.setImageUrl(String.format("%s/%s/%s", amazonS3Url, amazonS3Bucket, record.getImageKey()));
        }
        post.setCreatedAt(record.getCreatedAt());
        post.setUpdatedAt(record.getUpdatedAt());
        return post;
    }
}
