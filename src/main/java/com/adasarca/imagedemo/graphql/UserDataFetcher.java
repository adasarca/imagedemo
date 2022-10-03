package com.adasarca.imagedemo.graphql;

import com.adasarca.imagedemo.model.domain.Post;
import com.adasarca.imagedemo.model.domain.User;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.model.exception.AmazonS3Exception;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.service.UserService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.security.RolesAllowed;
import java.util.List;

@DgsComponent
public class UserDataFetcher {

    private final UserService userService;

    @Autowired
    public UserDataFetcher(UserService userService) {
        this.userService = userService;
    }

    @DgsQuery
    @RolesAllowed({RoleEnum.Authority.ADMIN})
    public User user(@InputArgument(name="userId") String userId) throws DatabaseException {
        return this.userService.findById(userId);
    }
}
