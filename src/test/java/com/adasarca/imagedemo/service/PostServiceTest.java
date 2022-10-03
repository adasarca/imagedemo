package com.adasarca.imagedemo.service;

import com.adasarca.imagedemo.model.database.PostRecord;
import com.adasarca.imagedemo.model.domain.ContextUser;
import com.adasarca.imagedemo.model.domain.Post;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.repository.PostRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PostServiceTest {

    private PostRepository postRepositoryMock;
    private AmazonS3Service amazonS3ServiceMock;

    private PostService postService;

    private ContextUser contextUser;

    private final String amazonS3Bucket = "imagedemo-posts";
    private final String amazonS3Url = "http://localhost:4566";

    @BeforeMethod
    public void setup() {
        postRepositoryMock = Mockito.mock(PostRepository.class);
        amazonS3ServiceMock = Mockito.mock(AmazonS3Service.class);

        this.postService = new PostService(postRepositoryMock, amazonS3ServiceMock);
        ReflectionTestUtils.setField(this.postService, "descriptionMaxLength", 300);
        ReflectionTestUtils.setField(this.postService, "allowedContentTypes", Arrays.asList("image/jpeg", "image/png"));
        ReflectionTestUtils.setField(this.postService, "amazonS3Url", amazonS3Url);
        ReflectionTestUtils.setField(this.postService, "amazonS3Bucket", amazonS3Bucket);

        this.contextUser = new ContextUser("userId", "username", "password", Collections.emptyList());
    }

    @Test
    public void testUpload() throws IOException {
        //setup
        byte[] imageContent = "test content".getBytes();
        String contentType = "image/jpeg";

        MultipartFile multipartFileMock = Mockito.mock(MultipartFile.class);
        doReturn(contentType).when(multipartFileMock).getContentType();
        doReturn("test.jpg").when(multipartFileMock).getOriginalFilename();
        doReturn(imageContent).when(multipartFileMock).getBytes();

        doNothing().when(amazonS3ServiceMock).upload(eq(amazonS3Bucket), anyString(), eq(imageContent), eq(contentType));
        doNothing().when(postRepositoryMock).save(any());

        //test
        Post post = this.postService.upload(contextUser, "description", multipartFileMock);

        //verify
        assertNotNull(post);
        assertEquals(post.getUserId(), contextUser.getUserId());
        verify(amazonS3ServiceMock, times(1)).upload(eq(amazonS3Bucket), anyString(), eq(imageContent), eq(contentType));
        verify(postRepositoryMock, times(1)).save(any());
    }

    @Test(expectedExceptions = { IllegalArgumentException.class } )
    public void testUpload_MissingContextUser() {
        //test
        this.postService.upload(null, "description", Mockito.mock(MultipartFile.class));
    }

    @Test(expectedExceptions = { ValidationException.class } )
    public void testUpload_MissingImageFile() {
        //test
        this.postService.upload(contextUser, "description", null);
    }

    @Test(expectedExceptions = { ValidationException.class } )
    public void testUpload_DescriptionTooLong() {
        //setup
        String longDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque ac purus ut dolor lacinia porta. Praesent iaculis est at dolor rutrum, vitae consectetur turpis sodales. Maecenas sagittis, orci ac sollicitudin feugiat, nibh tortor tincidunt purus, at finibus dolor sem in lectus. Pellentesque ultrices, quam quis vehicula accumsan, orci urna mollis ligula, nec finibus neque felis at quam. Aliquam libero dui, posuere et elit at, rhoncus tempus turpis. Vivamus vel quam quis augue dapibus hendrerit quis eget eros. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Mauris maximus convallis iaculis. Donec consequat elit at mattis cursus. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Maecenas ornare nunc et diam convallis, ac semper purus eleifend. Suspendisse nulla ante, auctor in molestie eu, consequat ac quam.\n" +
                "\n" +
                "Pellentesque a lorem feugiat, finibus nunc ac, eleifend ipsum. Fusce vitae justo non ligula ornare volutpat. Morbi turpis dui, semper et libero sed, dictum ultrices mi. Nulla pharetra tincidunt ultricies. Mauris arcu justo, maximus eu orci vitae, luctus suscipit nibh. Pellentesque elementum nisi eu justo gravida, vitae pulvinar metus pharetra. Sed augue tortor, semper non congue et, cursus condimentum neque. Pellentesque convallis, turpis et volutpat ultrices, mi nibh fringilla massa, at blandit odio lorem vitae dui. Sed eu mauris nisi. Aliquam pretium, ante in pellentesque volutpat, leo nibh auctor felis, at aliquet dui enim at nulla. Etiam sit amet purus diam. Nunc condimentum tellus ut congue blandit.\n" +
                "\n" +
                "Duis vitae sem nisl. Aenean sit amet porta lectus. Cras risus orci, faucibus eget elit ut, fermentum tempor nisl. Cras vel malesuada mauris, nec luctus est. Vestibulum molestie aliquet enim, id venenatis magna volutpat bibendum. Sed bibendum id quam sed lobortis. Cras facilisis sem a nulla finibus, nec commodo ipsum dapibus. Nam imperdiet accumsan odio nec mollis. Pellentesque imperdiet non lorem in tempus. Sed hendrerit quis diam vel ultrices. Aliquam ultrices ultricies nisi quis luctus.\n" +
                "\n" +
                "Phasellus a dolor nec lorem lobortis sollicitudin. Vestibulum commodo nulla ac erat tincidunt fringilla. Aenean sit amet ex nec est auctor ornare. Sed sollicitudin gravida ex eu pulvinar. Pellentesque in metus nisl. Quisque tincidunt, massa vel dictum tristique, lectus mauris ullamcorper sem, ut pellentesque purus enim vel dolor. Quisque tincidunt elit ut pharetra porta. Donec pharetra sodales sapien in malesuada. Pellentesque tristique augue id convallis vehicula. Proin laoreet quis ipsum vel hendrerit. Phasellus bibendum magna non leo ultricies, ut sodales leo scelerisque. Phasellus vitae erat velit.\n" +
                "\n" +
                "Quisque fringilla sagittis porta. Donec sit amet tortor libero. Vestibulum id varius ante, at volutpat est. Aenean in maximus felis, at elementum lorem. Sed vel cursus odio, a tincidunt felis. Aliquam erat volutpat. Donec egestas auctor condimentum. Curabitur tincidunt mi risus, eget commodo massa rutrum a. Morbi vitae ipsum eget erat porta bibendum. Nunc elementum mi sit amet turpis luctus auctor. Nulla sed maximus justo. Aliquam nisi risus, mattis id pharetra posuere, vehicula nec tortor. Nunc id nunc luctus turpis euismod dignissim. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Integer in laoreet elit.";

        //test
        this.postService.upload(contextUser, longDescription, Mockito.mock(MultipartFile.class));
    }

    @Test(expectedExceptions = { ValidationException.class } )
    public void testUpload_InvalidContentType() {
        //setup
        MultipartFile multipartFileMock = Mockito.mock(MultipartFile.class);
        doReturn("image/gif").when(multipartFileMock).getContentType();

        //test
        this.postService.upload(contextUser, "description", multipartFileMock);
    }

    @Test(expectedExceptions = { ValidationException.class } )
    public void testUpload_InvalidContent() throws IOException {
        //setup
        MultipartFile multipartFileMock = Mockito.mock(MultipartFile.class);
        doReturn("image/jpeg").when(multipartFileMock).getContentType();
        doReturn("test.jpg").when(multipartFileMock).getOriginalFilename();
        doThrow(new IOException("test")).when(multipartFileMock).getBytes();

        //test
        this.postService.upload(contextUser, "description", multipartFileMock);
    }

    @Test
    public void testUpload_DatabaseException() throws IOException {
        //setup
        byte[] imageContent = "test content".getBytes();
        String contentType = "image/jpeg";
        DatabaseException expectedException = new DatabaseException(new Exception("test"));

        MultipartFile multipartFileMock = Mockito.mock(MultipartFile.class);
        doReturn(contentType).when(multipartFileMock).getContentType();
        doReturn("test.jpg").when(multipartFileMock).getOriginalFilename();
        doReturn(imageContent).when(multipartFileMock).getBytes();

        doNothing().when(amazonS3ServiceMock).upload(eq(amazonS3Bucket), anyString(), eq(imageContent), eq(contentType));
        doThrow(expectedException).when(postRepositoryMock).save(any());
        doNothing().when(amazonS3ServiceMock).delete(eq(amazonS3Bucket), anyString());

        //test
        DatabaseException exception = null;
        try {
            this.postService.upload(contextUser, "description", multipartFileMock);
        } catch (DatabaseException e) {
            exception = e;
        }

        //verify
        assertEquals(exception, expectedException);
        verify(amazonS3ServiceMock, times(1)).upload(eq(amazonS3Bucket), anyString(), eq(imageContent), eq(contentType));
        verify(amazonS3ServiceMock, times(1)).delete(eq(amazonS3Bucket), anyString());
    }

    @Test
    public void testFindByUser() {
        //setup
        HashMap<String, PostRecord> expectedRecords = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            PostRecord record = new PostRecord();
            record.setUserId(contextUser.getUserId());
            record.setPostId("post-id-" + i);
            record.setDescription("test description " + i);
            record.setImageKey("test-image-" + i);
            record.setCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
            record.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
            expectedRecords.put(record.getUserId() + record.getPostId(), record);
        }
        doReturn(new LinkedList<>(expectedRecords.values())).when(postRepositoryMock).findAllByUserId(contextUser.getUserId());

        //test
        List<Post> posts = this.postService.findAllByUser(contextUser);

        //verify
        assertPosts(posts, expectedRecords);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testFindByUser_MissingContextUser() {
        this.postService.findAllByUser(null);
    }

    @Test
    public void testFindByUser_EmptyList() {
        //setup
        doReturn(new LinkedList<>()).when(postRepositoryMock).findAllByUserId(contextUser.getUserId());

        //test
        List<Post> posts = this.postService.findAllByUser(contextUser);

        //verify
        assertPosts(posts, new HashMap<>());
    }

    @Test
    public void testFindByUser_NullList() {
        //setup
        doReturn(null).when(postRepositoryMock).findAllByUserId(contextUser.getUserId());

        //test
        List<Post> posts = this.postService.findAllByUser(contextUser);

        //verify
        assertNull(posts);
    }

    @Test
    public void testUpdate() {
        //setup
        String postId = "post-id";
        String description = "new description";

        PostRecord postRecord = new PostRecord();
        postRecord.setUserId(contextUser.getUserId());
        postRecord.setPostId(postId);
        postRecord.setDescription("old description");
        doReturn(postRecord).when(postRepositoryMock).findByPostId(contextUser.getUserId(), postId);

        //test
        Post post = this.postService.update(contextUser, postId, description);

        //verify
        assertNotNull(post);
        assertEquals(post.getUserId(), postRecord.getUserId());
        assertEquals(post.getPostId(), postRecord.getPostId());
        assertEquals(post.getDescription(), description);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class } )
    public void testUpdate_MissingContextUser() {
        //test
        this.postService.update(null, "post-id", "description");
    }

    @Test(expectedExceptions = { ValidationException.class } )
    public void testUpdate_DescriptionTooLong() {
        //setup
        String longDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque ac purus ut dolor lacinia porta. Praesent iaculis est at dolor rutrum, vitae consectetur turpis sodales. Maecenas sagittis, orci ac sollicitudin feugiat, nibh tortor tincidunt purus, at finibus dolor sem in lectus. Pellentesque ultrices, quam quis vehicula accumsan, orci urna mollis ligula, nec finibus neque felis at quam. Aliquam libero dui, posuere et elit at, rhoncus tempus turpis. Vivamus vel quam quis augue dapibus hendrerit quis eget eros. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Mauris maximus convallis iaculis. Donec consequat elit at mattis cursus. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Maecenas ornare nunc et diam convallis, ac semper purus eleifend. Suspendisse nulla ante, auctor in molestie eu, consequat ac quam.\n" +
                "\n" +
                "Pellentesque a lorem feugiat, finibus nunc ac, eleifend ipsum. Fusce vitae justo non ligula ornare volutpat. Morbi turpis dui, semper et libero sed, dictum ultrices mi. Nulla pharetra tincidunt ultricies. Mauris arcu justo, maximus eu orci vitae, luctus suscipit nibh. Pellentesque elementum nisi eu justo gravida, vitae pulvinar metus pharetra. Sed augue tortor, semper non congue et, cursus condimentum neque. Pellentesque convallis, turpis et volutpat ultrices, mi nibh fringilla massa, at blandit odio lorem vitae dui. Sed eu mauris nisi. Aliquam pretium, ante in pellentesque volutpat, leo nibh auctor felis, at aliquet dui enim at nulla. Etiam sit amet purus diam. Nunc condimentum tellus ut congue blandit.\n" +
                "\n" +
                "Duis vitae sem nisl. Aenean sit amet porta lectus. Cras risus orci, faucibus eget elit ut, fermentum tempor nisl. Cras vel malesuada mauris, nec luctus est. Vestibulum molestie aliquet enim, id venenatis magna volutpat bibendum. Sed bibendum id quam sed lobortis. Cras facilisis sem a nulla finibus, nec commodo ipsum dapibus. Nam imperdiet accumsan odio nec mollis. Pellentesque imperdiet non lorem in tempus. Sed hendrerit quis diam vel ultrices. Aliquam ultrices ultricies nisi quis luctus.\n" +
                "\n" +
                "Phasellus a dolor nec lorem lobortis sollicitudin. Vestibulum commodo nulla ac erat tincidunt fringilla. Aenean sit amet ex nec est auctor ornare. Sed sollicitudin gravida ex eu pulvinar. Pellentesque in metus nisl. Quisque tincidunt, massa vel dictum tristique, lectus mauris ullamcorper sem, ut pellentesque purus enim vel dolor. Quisque tincidunt elit ut pharetra porta. Donec pharetra sodales sapien in malesuada. Pellentesque tristique augue id convallis vehicula. Proin laoreet quis ipsum vel hendrerit. Phasellus bibendum magna non leo ultricies, ut sodales leo scelerisque. Phasellus vitae erat velit.\n" +
                "\n" +
                "Quisque fringilla sagittis porta. Donec sit amet tortor libero. Vestibulum id varius ante, at volutpat est. Aenean in maximus felis, at elementum lorem. Sed vel cursus odio, a tincidunt felis. Aliquam erat volutpat. Donec egestas auctor condimentum. Curabitur tincidunt mi risus, eget commodo massa rutrum a. Morbi vitae ipsum eget erat porta bibendum. Nunc elementum mi sit amet turpis luctus auctor. Nulla sed maximus justo. Aliquam nisi risus, mattis id pharetra posuere, vehicula nec tortor. Nunc id nunc luctus turpis euismod dignissim. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Integer in laoreet elit.";

        //test
        this.postService.update(contextUser,"post-id", longDescription);
    }

    @Test(expectedExceptions = { ValidationException.class } )
    public void testUpdate_PostNotFound() {
        //setup
        String postId = "post-id";
        doReturn(null).when(postRepositoryMock).findByPostId(contextUser.getUserId(), postId);

        //test
        this.postService.update(contextUser, postId, "description");
    }

    @Test
    public void testDelete() {
        //setup
        String postId = "post-id";
        PostRecord record = new PostRecord();
        record.setImageKey("image-key");

        doReturn(record).when(this.postRepositoryMock).findByPostId(contextUser.getUserId(), postId);
        doNothing().when(this.postRepositoryMock).delete(contextUser.getUserId(), postId);
        doNothing().when(this.amazonS3ServiceMock).delete(amazonS3Bucket, record.getImageKey());

        //test
        this.postService.delete(contextUser, postId);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testDelete_MissingContextUser() {
        this.postService.delete(null, "post-id");
    }

    @Test(expectedExceptions = { ValidationException.class })
    public void testDelete_PostNotFound() {
        //setup
        String postId = "post-id";
        doReturn(null).when(this.postRepositoryMock).findByPostId(contextUser.getUserId(), postId);

        //test
        this.postService.delete(contextUser, postId);
    }

    private void assertPosts(List<Post> posts, HashMap<String, PostRecord> records) {
        assertNotNull(posts);
        assertEquals(posts.size(), records.size());
        for (Post post : posts) {
            PostRecord record = records.get(post.getUserId() + post.getPostId());
            assertNotNull(record);
            assertEquals(post.getUserId(), record.getUserId());
            assertEquals(post.getPostId(), record.getPostId());
            assertEquals(post.getDescription(), record.getDescription());
            assertEquals(post.getCreatedAt(), record.getCreatedAt());
            assertEquals(post.getUpdatedAt(), record.getUpdatedAt());

            if (null == record.getImageKey())
                assertNull(post.getImageUrl());
            else
                assertEquals(post.getImageUrl(), String.format("%s/%s/%s", amazonS3Url, amazonS3Bucket, record.getImageKey()));
        }
    }
}