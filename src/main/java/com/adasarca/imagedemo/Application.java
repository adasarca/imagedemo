package com.adasarca.imagedemo;

import com.adasarca.imagedemo.model.domain.authentication.SignupRequest;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.service.PostService;
import com.adasarca.imagedemo.service.security.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostService.class);

    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(Application.class, args);

        AuthenticationService authenticationService = applicationContext.getBean(AuthenticationService.class);

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("admin@test.com");
        signupRequest.setPassword("Admin1234.");
        signupRequest.setFirstName("Admin");
        signupRequest.setLastName("Test");

        try {
            authenticationService.signup(signupRequest, RoleEnum.Admin);
        } catch (Exception exception) {
            LOGGER.error("Exception adding admin user", exception);
        }
    }
}
