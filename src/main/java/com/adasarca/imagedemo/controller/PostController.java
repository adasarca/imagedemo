package com.adasarca.imagedemo.controller;

import com.adasarca.imagedemo.model.domain.ContextUser;
import com.adasarca.imagedemo.model.domain.Post;
import com.adasarca.imagedemo.model.exception.AmazonS3Exception;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @RequestMapping(method = RequestMethod.POST)
    public Post upload(@RequestParam("description") String description, @RequestParam("image") MultipartFile image) throws ValidationException, AmazonS3Exception, DatabaseException {
        ContextUser currentUser = (ContextUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return this.postService.upload(currentUser, description, image);
    }
}
