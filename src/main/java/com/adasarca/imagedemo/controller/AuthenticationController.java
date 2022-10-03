package com.adasarca.imagedemo.controller;

import com.adasarca.imagedemo.model.domain.authentication.AuthenticationRequest;
import com.adasarca.imagedemo.model.domain.authentication.AuthenticationResponse;
import com.adasarca.imagedemo.model.domain.authentication.SignupRequest;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.service.security.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Autowired
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @RequestMapping(value = "/authentication", method = RequestMethod.POST)
    public AuthenticationResponse authenticate(@RequestBody AuthenticationRequest authenticationRequest) throws ValidationException {
        return this.authenticationService.authenticate(authenticationRequest);
    }

    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public void signup(@RequestBody SignupRequest signupRequest) throws ValidationException, DatabaseException {
        this.authenticationService.signup(signupRequest, RoleEnum.User);
    }
}
