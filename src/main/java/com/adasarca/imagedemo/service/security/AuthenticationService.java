package com.adasarca.imagedemo.service.security;

import com.adasarca.imagedemo.model.database.UserRecord;
import com.adasarca.imagedemo.model.database.UserCredentialsRecord;
import com.adasarca.imagedemo.model.domain.authentication.AuthenticationRequest;
import com.adasarca.imagedemo.model.domain.authentication.AuthenticationResponse;
import com.adasarca.imagedemo.model.domain.authentication.SignupRequest;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.adasarca.imagedemo.repository.UserCredentialsRepository;
import com.adasarca.imagedemo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final SecurityUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern passwordPattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[\\.!@#&()–\\[{}\\]:;',?/*~$^+=<>]).{8,50}$");

    private static final Pattern emailPattern = Pattern.compile("^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$");

    @Autowired
    public AuthenticationService(UserRepository userRepository, UserCredentialsRepository userCredentialsRepository, AuthenticationManager authenticationManager, JwtTokenService jwtTokenService, SecurityUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) throws ValidationException {
        if (null == authenticationRequest || !StringUtils.hasLength(authenticationRequest.getEmail()) || !StringUtils.hasLength(authenticationRequest.getPassword()))
            throw new ValidationException("Missing credentials");

        try {
            this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword()));
        } catch (BadCredentialsException e) {
            LOGGER.debug("Exception authenticating user: ", e);
            throw new ValidationException("Invalid credentials");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getEmail());
        String token = jwtTokenService.generateToken(userDetails);

        AuthenticationResponse authenticationResponse = new AuthenticationResponse();
        authenticationResponse.setToken(token);
        return authenticationResponse;
    }

    public void signup(SignupRequest signupRequest, RoleEnum role) throws ValidationException, DatabaseException {
        if (null == role)
            throw new IllegalArgumentException("role");

        if (null == signupRequest || !StringUtils.hasLength(signupRequest.getFirstName()) || !StringUtils.hasLength(signupRequest.getLastName())
                || !StringUtils.hasLength(signupRequest.getEmail()) || !StringUtils.hasLength(signupRequest.getPassword()))
            throw new ValidationException("Missing required fields");

        //validate email
        Matcher emailMatcher = emailPattern.matcher(signupRequest.getEmail());
        if (!emailMatcher.matches())
            throw new ValidationException("Invalid email address");

        //check for duplicate email
        UserCredentialsRecord existingUserCredentialsRecord = userCredentialsRepository.findByEmail(signupRequest.getEmail());
        if (existingUserCredentialsRecord != null)
            throw new ValidationException("Email address already exists");

        //validate password
        Matcher passwordMatcher = passwordPattern.matcher(signupRequest.getPassword());
        if (!passwordMatcher.matches())
            throw new ValidationException("Password must contain at least 8 characters of which at least one digit, one lowercase letter, one uppercase letter and one special character");

        //save UserCredentials
        String userId = UUID.randomUUID().toString();
        UserCredentialsRecord userCredentialsRecord = new UserCredentialsRecord();
        userCredentialsRecord.setUserId(userId);
        userCredentialsRecord.setEmail(signupRequest.getEmail());
        userCredentialsRecord.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        userCredentialsRepository.insert(userCredentialsRecord);

        //save User
        //todo: integrate @Transactional with enhanced dynamodb client
        UserRecord userRecord = new UserRecord();
        userRecord.setId(userId);
        userRecord.setFirstName(signupRequest.getFirstName());
        userRecord.setLastName(signupRequest.getLastName());
        userRecord.setRoleId(role.getId());
        userRecord.setCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")));

        try {
            userRepository.insert(userRecord);
        } catch (Exception e) {
            LOGGER.debug("Exception inserting User, rolling back UserCredentials insert...");
            userCredentialsRepository.delete(userId);
            throw e;
        }
    }
}
