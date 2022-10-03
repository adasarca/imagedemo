package com.adasarca.imagedemo.graphql;

import com.adasarca.imagedemo.model.domain.ContextUser;
import com.adasarca.imagedemo.model.domain.Post;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.model.exception.AmazonS3Exception;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.service.PostService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@DgsComponent
public class PostDataFetcher {

    private final PostService postService;

    @Autowired
    public PostDataFetcher(PostService postService) {
        this.postService = postService;
    }

    @DgsQuery
    public List<Post> posts() throws DatabaseException {
        ContextUser currentUser = (ContextUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return postService.findAllByUser(currentUser);
    }

    @DgsQuery
    public List<Post> completedPosts() throws DatabaseException {
        ContextUser currentUser = (ContextUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return postService.findCompletedByUser(currentUser);
    }

    @DgsMutation
    public Post uploadPost(@InputArgument(name="description") String description, @InputArgument(name = "image") MultipartFile inputFile) throws ValidationException, AmazonS3Exception, DatabaseException {
        ContextUser currentUser = (ContextUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return this.postService.upload(currentUser, description, inputFile);
    }

    @DgsMutation
    public String createPost(@InputArgument(name="description") String description) throws ValidationException, AmazonS3Exception, DatabaseException {
        ContextUser currentUser = (ContextUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return this.postService.create(currentUser, description);
    }

    @DgsMutation
    public Post updatePost(@InputArgument(name="postId") String postId, @InputArgument(name="description") String description) throws ValidationException, DatabaseException {
        ContextUser currentUser = (ContextUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return this.postService.update(currentUser, postId, description);
    }

    @DgsMutation
    public String deletePost(@InputArgument(name="postId") String postId) throws ValidationException, AmazonS3Exception, DatabaseException {
        ContextUser currentUser = (ContextUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        this.postService.delete(currentUser, postId);
        return postId;
    }
}
