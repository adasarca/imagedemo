package com.adasarca.imagedemo.service.security;

import com.adasarca.imagedemo.model.database.UserRecord;
import com.adasarca.imagedemo.model.database.UserCredentialsRecord;
import com.adasarca.imagedemo.model.domain.ContextUser;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.repository.UserCredentialsRepository;
import com.adasarca.imagedemo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SecurityUserDetailsService implements UserDetailsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityUserDetailsService.class);

    private final UserCredentialsRepository userCredentialsRepository;
    private final UserRepository userRepository;

    @Autowired
    public SecurityUserDetailsService(UserCredentialsRepository userCredentialsRepository, UserRepository userRepository) {
        this.userCredentialsRepository = userCredentialsRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserCredentialsRecord userCredentialsRecord = null;
        UserRecord userRecord = null;
        try {
            userCredentialsRecord = userCredentialsRepository.findByEmail(username);
            if (userCredentialsRecord != null) {
                userRecord = userRepository.findById(userCredentialsRecord.getUserId());
            }
        } catch (DatabaseException exception) {
            LOGGER.error("Exception loading UserCredentials from database: ", exception);
        }

        if (null == userCredentialsRecord || null == userRecord)
            throw new UsernameNotFoundException("User not found with username: " + username);

        RoleEnum roleEnum = RoleEnum.getById(userRecord.getRoleId());
        List<GrantedAuthority> grantedAuthorities = roleEnum != null ? Collections.singletonList(new SimpleGrantedAuthority(roleEnum.getAuthority())) : Collections.emptyList();

        return new ContextUser(userCredentialsRecord.getUserId(), userCredentialsRecord.getEmail(), userCredentialsRecord.getPassword(), grantedAuthorities);
    }
}
