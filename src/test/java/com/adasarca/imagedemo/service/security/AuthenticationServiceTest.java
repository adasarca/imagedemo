package com.adasarca.imagedemo.service.security;

import com.adasarca.imagedemo.model.database.UserRecord;
import com.adasarca.imagedemo.model.database.UserCredentialsRecord;
import com.adasarca.imagedemo.model.domain.ContextUser;
import com.adasarca.imagedemo.model.domain.authentication.AuthenticationRequest;
import com.adasarca.imagedemo.model.domain.authentication.AuthenticationResponse;
import com.adasarca.imagedemo.model.domain.authentication.SignupRequest;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.repository.UserCredentialsRepository;
import com.adasarca.imagedemo.repository.UserRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class AuthenticationServiceTest {

    private UserRepository userRepositoryMock;
    private UserCredentialsRepository userCredentialsRepositoryMock;
    private AuthenticationManager authenticationManagerMock;
    private JwtTokenService jwtTokenServiceMock;
    private SecurityUserDetailsService userDetailsServiceMock;
    private PasswordEncoder passwordEncoderMock;

    private AuthenticationService authenticationService;

    @BeforeMethod
    public void setup() {
        this.userRepositoryMock = mock(UserRepository.class);
        this.userCredentialsRepositoryMock = mock(UserCredentialsRepository.class);
        this.authenticationManagerMock = mock(AuthenticationManager.class);
        this.jwtTokenServiceMock = mock(JwtTokenService.class);
        this.userDetailsServiceMock = mock(SecurityUserDetailsService.class);
        this.passwordEncoderMock = mock(PasswordEncoder.class);

        this.authenticationService = new AuthenticationService(userRepositoryMock, userCredentialsRepositoryMock, authenticationManagerMock, jwtTokenServiceMock, userDetailsServiceMock, passwordEncoderMock);
    }

    @Test
    public void testAuthenticate() {
        //setup
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setEmail("email@test.com");
        authenticationRequest.setPassword("testpassword");

        doReturn(mock(Authentication.class)).when(this.authenticationManagerMock).authenticate(any());

        UserDetails userDetails = new ContextUser("userId", "email@test.com", "password", Collections.emptyList());
        doReturn(userDetails).when(this.userDetailsServiceMock).loadUserByUsername(authenticationRequest.getEmail());
        String token = "testToken";
        doReturn(token).when(this.jwtTokenServiceMock).generateToken(userDetails);

        //test
        AuthenticationResponse response = this.authenticationService.authenticate(authenticationRequest);

        //verify
        assertNotNull(response);
        assertEquals(response.getToken(), token);
    }

    @Test(expectedExceptions = { ValidationException.class })
    public void testAuthenticate_NullRequest() {
        this.authenticationService.authenticate(null);
    }

    @Test(expectedExceptions = { ValidationException.class })
    public void testAuthenticate_EmptyRequest() {
        this.authenticationService.authenticate(new AuthenticationRequest());
    }

    @Test(expectedExceptions = { ValidationException.class })
    public void testAuthenticate_InvalidCredentials() {
        //setup
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setEmail("email@test.com");
        authenticationRequest.setPassword("testpassword");

        doThrow(new BadCredentialsException("test")).when(this.authenticationManagerMock).authenticate(any());

        //test
        this.authenticationService.authenticate(authenticationRequest);
    }

    @Test
    public void testSignup() {
        //setup
        SignupRequest signupRequest = this.buildTestSignupRequest();
        RoleEnum role = RoleEnum.User;

        doReturn(null).when(userCredentialsRepositoryMock).findByEmail(signupRequest.getEmail());

        String encodedPassword = "qwertyuiopasdfghjklzxcvbm";
        doReturn(encodedPassword).when(passwordEncoderMock).encode(signupRequest.getPassword());

        doNothing().when(userCredentialsRepositoryMock).insert(any());
        doNothing().when(userRepositoryMock).insert(any());

        //test
        this.authenticationService.signup(signupRequest, role);

        //verify
        ArgumentCaptor<UserCredentialsRecord> userCredentialsCaptor = ArgumentCaptor.forClass(UserCredentialsRecord.class);
        ArgumentCaptor<UserRecord> userCaptor = ArgumentCaptor.forClass(UserRecord.class);
        verify(userCredentialsRepositoryMock, times(1)).insert(userCredentialsCaptor.capture());
        verify(userRepositoryMock, times(1)).insert(userCaptor.capture());

        UserCredentialsRecord userCredentialsRecord = userCredentialsCaptor.getValue();
        UserRecord userRecord = userCaptor.getValue();
        assertNotNull(userCredentialsRecord);
        assertNotNull(userRecord);

        assertNotNull(userRecord.getId());
        assertEquals(userRecord.getFirstName(), signupRequest.getFirstName());
        assertEquals(userRecord.getLastName(), signupRequest.getLastName());
        assertEquals(userRecord.getRoleId(), role.getId());

        assertEquals(userCredentialsRecord.getUserId(), userRecord.getId());
        assertEquals(userCredentialsRecord.getEmail(), signupRequest.getEmail());
        assertEquals(userCredentialsRecord.getPassword(), encodedPassword);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testSignup_MissingRole() {
        this.authenticationService.signup(new SignupRequest(), null);
    }

    @Test(expectedExceptions = { ValidationException.class })
    public void testSignup_NullRequest() {
        this.authenticationService.signup(null, RoleEnum.User);
    }

    @Test(expectedExceptions = { ValidationException.class })
    public void testSignup_EmptyRequest() {
        this.authenticationService.signup(new SignupRequest(), RoleEnum.User);
    }

    @Test(expectedExceptions = { ValidationException.class })
    public void testSignup_InvalidEmail() {
        //setup
        SignupRequest signupRequest = this.buildTestSignupRequest();
        signupRequest.setEmail("asdfghjkl");

        //test
        this.authenticationService.signup(signupRequest, RoleEnum.User);
    }

    @Test(expectedExceptions = { ValidationException.class })
    public void testSignup_DuplicateEmail() {
        //setup
        SignupRequest signupRequest = this.buildTestSignupRequest();
        doReturn(new UserCredentialsRecord()).when(userCredentialsRepositoryMock).findByEmail(signupRequest.getEmail());

        //test
        this.authenticationService.signup(signupRequest, RoleEnum.User);
    }

    @Test(expectedExceptions = { ValidationException.class })
    public void testSignup_InvalidPassword() {
        //setup
        SignupRequest signupRequest = this.buildTestSignupRequest();
        signupRequest.setPassword("hello");

        doReturn(null).when(userCredentialsRepositoryMock).findByEmail(signupRequest.getEmail());

        //test
        this.authenticationService.signup(signupRequest, RoleEnum.User);
    }

    @Test
    public void testSignup_DatabaseException() {
        //setup
        SignupRequest signupRequest = this.buildTestSignupRequest();

        doReturn(null).when(userCredentialsRepositoryMock).findByEmail(signupRequest.getEmail());

        String encodedPassword = "qwertyuiopasdfghjklzxcvbm";
        doReturn(encodedPassword).when(passwordEncoderMock).encode(signupRequest.getPassword());

        DatabaseException expectedException = new DatabaseException(new Exception("test"));
        doNothing().when(userCredentialsRepositoryMock).insert(any());
        doThrow(expectedException).when(userRepositoryMock).insert(any());

        //test
        DatabaseException databaseException = null;
        try {
            this.authenticationService.signup(signupRequest, RoleEnum.User);
        } catch (DatabaseException e) {
            databaseException = e;
        }

        //verify
        assertNotNull(databaseException);
        assertEquals(databaseException, expectedException);

        verify(userCredentialsRepositoryMock, times(1)).insert(any());
        verify(userRepositoryMock, times(1)).insert(any());
        verify(userCredentialsRepositoryMock, times(1)).delete(any());
    }

    private SignupRequest buildTestSignupRequest() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("email@test.com");
        signupRequest.setPassword("TestPassword1234.");
        signupRequest.setFirstName("Albert");
        signupRequest.setLastName("Einstein");
        return signupRequest;
    }
}