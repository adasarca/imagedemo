package com.adasarca.imagedemo.controller;

import com.adasarca.imagedemo.BaseSpringContextTest;
import com.adasarca.imagedemo.model.domain.ErrorResponse;
import com.adasarca.imagedemo.model.domain.authentication.AuthenticationRequest;
import com.adasarca.imagedemo.model.domain.authentication.AuthenticationResponse;
import com.adasarca.imagedemo.model.domain.authentication.SignupRequest;
import com.adasarca.imagedemo.model.enumeration.ErrorEnum;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.service.security.AuthenticationService;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationControllerTest extends BaseSpringContextTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @MockBean
    private AuthenticationService authenticationService;

    @Test
    public void testAuthenticate() {
        //setup
        AuthenticationRequest expectedRequest = new AuthenticationRequest();
        expectedRequest.setEmail("email@test.com");
        expectedRequest.setPassword("testpassword");

        AuthenticationResponse expectedResponse = new AuthenticationResponse();
        expectedResponse.setToken("abcdefghijklmnopqrstuvwxyz");
        doReturn(expectedResponse).when(authenticationService).authenticate(any());

        //test
        ResponseEntity<AuthenticationResponse> response = restTemplate.postForEntity("/authentication", expectedRequest, AuthenticationResponse.class);

        //verify
        assertNotNull(response);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getToken(), expectedResponse.getToken());

        ArgumentCaptor<AuthenticationRequest> argumentCaptor = ArgumentCaptor.forClass(AuthenticationRequest.class);
        verify(authenticationService, times(1)).authenticate(argumentCaptor.capture());
        AuthenticationRequest authenticationRequest = argumentCaptor.getValue();

        assertNotNull(authenticationRequest);
        assertEquals(authenticationRequest.getEmail(), expectedRequest.getEmail());
        assertEquals(authenticationRequest.getPassword(), expectedRequest.getPassword());
    }

    @Test
    public void testAuthenticate_ValidationException() {
        //setup
        String validationMessage = "Test Error";
        doThrow(new ValidationException(validationMessage)).when(authenticationService).authenticate(any());

        //test
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/authentication", new AuthenticationRequest(), ErrorResponse.class);

        //verify
        assertNotNull(response);
        assertEquals(response.getStatusCode(), ErrorEnum.ValidationError.getHttpStatus());
        assertErrorResponse(response.getBody(), ErrorEnum.ValidationError, validationMessage);
    }

    @Test
    public void testSignup() {
        //setup
        SignupRequest expectedSignupRequest = new SignupRequest();
        expectedSignupRequest.setFirstName("Test");
        expectedSignupRequest.setLastName("Example");
        expectedSignupRequest.setEmail("email@test.com");
        expectedSignupRequest.setPassword("testpassword");
        doNothing().when(authenticationService).signup(any(), eq(RoleEnum.User));

        //test
        ResponseEntity<Object> response = restTemplate.postForEntity("/signup", expectedSignupRequest, Object.class);

        //verify
        assertNotNull(response);
        assertEquals(response.getStatusCode(), HttpStatus.OK);

        ArgumentCaptor<SignupRequest> argumentCaptor = ArgumentCaptor.forClass(SignupRequest.class);
        verify(authenticationService, times(1)).signup(argumentCaptor.capture(), eq(RoleEnum.User));
        SignupRequest signupRequest = argumentCaptor.getValue();

        assertNotNull(signupRequest);
        assertEquals(signupRequest.getFirstName(), expectedSignupRequest.getFirstName());
        assertEquals(signupRequest.getLastName(), expectedSignupRequest.getLastName());
        assertEquals(signupRequest.getEmail(), expectedSignupRequest.getEmail());
        assertEquals(signupRequest.getPassword(), expectedSignupRequest.getPassword());
    }

    @Test
    public void testSignup_ValidationException() {
        //setup
        String validationMessage = "Test Error";
        doThrow(new ValidationException(validationMessage)).when(authenticationService).signup(any(), eq(RoleEnum.User));

        //test
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/signup", new SignupRequest(), ErrorResponse.class);

        //verify
        assertNotNull(response);
        assertEquals(response.getStatusCode(), ErrorEnum.ValidationError.getHttpStatus());
        assertErrorResponse(response.getBody(), ErrorEnum.ValidationError, validationMessage);
    }

    @Test
    public void testSignup_DatabaseException() {
        //setup
        doThrow(new DatabaseException(new Exception("test"))).when(authenticationService).signup(any(), eq(RoleEnum.User));

        //test
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/signup", new SignupRequest(), ErrorResponse.class);

        //verify
        assertNotNull(response);
        assertEquals(response.getStatusCode(), ErrorEnum.DatabaseError.getHttpStatus());
        assertErrorResponse(response.getBody(), ErrorEnum.DatabaseError, null);
    }

    private void assertErrorResponse(ErrorResponse errorResponse, ErrorEnum errorEnum, String errorDetails) {
        assertNotNull(errorResponse);
        assertEquals(errorResponse.getHttpCode(), errorEnum.getHttpStatus().value());
        assertEquals(errorResponse.getErrorCode(), errorEnum.getErrorCode());
        assertEquals(errorResponse.getMessage(), errorEnum.getMessage());
        assertEquals(errorResponse.getDetails(), errorDetails);
    }
}