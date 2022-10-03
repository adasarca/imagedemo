package com.adasarca.imagedemo;

import com.adasarca.imagedemo.model.domain.ContextUser;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class BaseSpringContextTest extends AbstractTestNGSpringContextTests {

    protected ContextUser contextUser;

    @BeforeClass
    public void init() {
        this.contextUser = new ContextUser("userId", "username", "password", Collections.singletonList(new SimpleGrantedAuthority(getContextUserAuthority())));
        SecurityContext securityContext = mock(SecurityContext.class);
        doReturn(new UsernamePasswordAuthenticationToken(contextUser, null, contextUser.getAuthorities())).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }

    protected String getContextUserAuthority() {
        return RoleEnum.User.getAuthority();
    }
}
