package com.adasarca.imagedemo.graphql;

import com.adasarca.imagedemo.BaseSpringContextTest;
import com.adasarca.imagedemo.model.database.PostRecord;
import com.adasarca.imagedemo.model.domain.Post;
import com.adasarca.imagedemo.model.enumeration.ErrorEnum;
import com.adasarca.imagedemo.model.exception.AmazonS3Exception;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.service.PostService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PostDataFetcherTest extends BaseSpringContextTest {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @Autowired
    @MockBean
    PostService postServiceMock;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeClass
    public void init() {
        super.init();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void testPosts() {
        //setup
        Post expectedPost = new Post();
        expectedPost.setUserId(this.contextUser.getUserId());
        expectedPost.setPostId("postId");
        doReturn(Collections.singletonList(expectedPost)).when(postServiceMock).findAllByUser(any());

        //test
        ExecutionResult result = dgsQueryExecutor.execute("{ posts { userId postId }}");

        //verify
        assertNotNull(result);
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertNotNull(result.getData());

        Map<String, Object> map = result.getData();
        assertNotNull(map.get("posts"));

        List<Post> posts = objectMapper.convertValue(map.get("posts"), new TypeReference<List<Post>>() {});
        assertNotNull(posts);
        assertEquals(posts.size(), 1);

        Post post = posts.get(0);
        assertEquals(post.getUserId(), expectedPost.getUserId());
        assertEquals(post.getPostId(), expectedPost.getPostId());
    }

    @Test
    public void testPosts_DatabaseException() {
        //setup
        doThrow(new DatabaseException(new Exception("test exception"))).when(postServiceMock).findAllByUser(any());

        //test
        ExecutionResult result = dgsQueryExecutor.execute("{ posts { userId postId }}");

        //verify
        assertNotNull(result);
        assertNotNull(result.getErrors());
        assertEquals(result.getErrors().size(), 1);
        assertTrue(result.getErrors().get(0).getMessage().startsWith(Integer.toString(ErrorEnum.DatabaseError.getErrorCode())));
    }

    @Test
    public void testUploadPost() {
        //setup
        MockMultipartFile mockMultipartFile = new MockMultipartFile("image.jpg", "image.jpg", "image/jpeg", "test".getBytes());
        String description = "TestDescription";

        Map<String, Object> variables = new HashMap<>();
        variables.put("description", description);
        variables.put("image", mockMultipartFile);

        Post expectedPost = new Post();
        expectedPost.setUserId(this.contextUser.getUserId());
        expectedPost.setPostId("postId");
        expectedPost.setImageUrl("testUrl");
        doReturn(expectedPost).when(postServiceMock).upload(any(), eq(description), any());

        //test
        ExecutionResult result = dgsQueryExecutor.execute("mutation UploadPost($description: String, $image: Upload!) { uploadPost(description: $description, image: $image) { userId postId imageUrl } }",
                variables);

        //verify
        assertNotNull(result);
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertNotNull(result.getData());

        Map<String, Object> map = result.getData();
        assertNotNull(map.get("uploadPost"));

        Post post = objectMapper.convertValue(map.get("uploadPost"), new TypeReference<Post>() {});
        assertNotNull(post);
        assertEquals(post.getUserId(), expectedPost.getUserId());
        assertEquals(post.getPostId(), expectedPost.getPostId());
        assertEquals(post.getImageUrl(), expectedPost.getImageUrl());
    }

    @Test
    public void testUploadPost_ValidationException() {
        //setup
        String description = "TestDescription";
        Map<String, Object> variables = new HashMap<>();
        variables.put("description", description);
        variables.put("image", null);

        String validationErrorMessage = "test validation";
        doThrow(new ValidationException(validationErrorMessage)).when(postServiceMock).upload(any(), eq(description), eq(null));

        //test
        ExecutionResult result = dgsQueryExecutor.execute("mutation UploadPost($description: String, $image: Upload!) { uploadPost(description: $description, image: $image) { userId postId imageUrl } }",
                variables);

        //verify
        assertNotNull(result);
        assertNotNull(result.getErrors());
        assertEquals(result.getErrors().size(), 1);
        assertTrue(result.getErrors().get(0).getMessage().contains("has coerced Null value for NonNull type"));
    }

    @Test
    public void testUploadPost_AmazonS3Exception() {
        //setup
        MockMultipartFile mockMultipartFile = new MockMultipartFile("image.jpg", "image.jpg", "image/jpeg", "test".getBytes());
        String description = "TestDescription";

        Map<String, Object> variables = new HashMap<>();
        variables.put("description", description);
        variables.put("image", mockMultipartFile);

        doThrow(new AmazonS3Exception(new Exception("test"))).when(postServiceMock).upload(any(), eq(description), any());

        //test
        ExecutionResult result = dgsQueryExecutor.execute("mutation UploadPost($description: String, $image: Upload!) { uploadPost(description: $description, image: $image) { userId postId imageUrl } }",
                variables);

        //verify
        assertNotNull(result);
        assertNotNull(result.getErrors());
        assertEquals(result.getErrors().size(), 1);
        assertTrue(result.getErrors().get(0).getMessage().startsWith(Integer.toString(ErrorEnum.AmazonS3Error.getErrorCode())));
    }

    @Test
    public void testUpdatePost() {
        //setup
        String postId = "post-id";
        String description = "test description";

        Map<String, Object> variables = new HashMap<>();
        variables.put("description", description);
        variables.put("postId", postId);

        Post expectedPost = new Post();
        expectedPost.setUserId(this.contextUser.getUserId());
        expectedPost.setPostId(postId);
        expectedPost.setDescription(description);
        expectedPost.setImageUrl("test-url");
        doReturn(expectedPost).when(this.postServiceMock).update(any(), eq(postId), eq(description));

        //test
        ExecutionResult result = dgsQueryExecutor.execute("mutation UpdatePost($postId: String!, $description: String) { updatePost(postId: $postId, description: $description) { userId postId imageUrl description } }",
                variables);

        //verify
        assertNotNull(result);
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertNotNull(result.getData());

        Map<String, Object> map = result.getData();
        assertNotNull(map.get("updatePost"));

        Post post = objectMapper.convertValue(map.get("updatePost"), new TypeReference<Post>() {});
        assertNotNull(post);
        assertEquals(post.getUserId(), expectedPost.getUserId());
        assertEquals(post.getPostId(), expectedPost.getPostId());
        assertEquals(post.getImageUrl(), expectedPost.getImageUrl());
        assertEquals(post.getDescription(), expectedPost.getDescription());
    }

    @Test
    public void testDeletePost() {
        //setup
        String postId = "post-id";

        Map<String, Object> variables = new HashMap<>();
        variables.put("postId", postId);

        doNothing().when(this.postServiceMock).delete(any(), eq(postId));

        //test
        ExecutionResult result = dgsQueryExecutor.execute("mutation DeletePost($postId: String!) { deletePost(postId: $postId) }",
                variables);

        //verify
        assertNotNull(result);
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertNotNull(result.getData());

        Map<String, Object> map = result.getData();
        assertEquals(map.get("deletePost"), postId);
    }
}