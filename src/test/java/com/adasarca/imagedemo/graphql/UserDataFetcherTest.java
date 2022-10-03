package com.adasarca.imagedemo.graphql;

import com.adasarca.imagedemo.BaseSpringContextTest;
import com.adasarca.imagedemo.model.domain.Post;
import com.adasarca.imagedemo.model.domain.User;
import com.adasarca.imagedemo.model.enumeration.ErrorEnum;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class UserDataFetcherTest extends BaseSpringContextTest {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @Autowired
    @MockBean
    UserService userServiceMock;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeClass
    public void init() {
        super.init();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected String getContextUserAuthority() {
        return RoleEnum.Authority.ADMIN;
    }

    @Test
    public void testUser() {
        //setup
        String userId = "user-id";

        User expectedUser = new User();
        expectedUser.setUserId("user-id");
        expectedUser.setFirstName("Albert");
        expectedUser.setLastName("Einstein");
        expectedUser.setRole(RoleEnum.User);
        expectedUser.setCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")));

        doReturn(expectedUser).when(userServiceMock).findById(userId);

        //test
        ExecutionResult result = dgsQueryExecutor.execute(String.format("{ user(userId: \"%s\") { userId firstName lastName role createdAt }}", userId));

        //verify
        assertNotNull(result);
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertNotNull(result.getData());

        Map<String, Object> map = result.getData();
        assertNotNull(map.get("user"));

        User user = objectMapper.convertValue(map.get("user"), new TypeReference<User>() {});
        assertNotNull(user);
        assertEquals(user.getUserId(), expectedUser.getUserId());
        assertEquals(user.getFirstName(), expectedUser.getFirstName());
        assertEquals(user.getLastName(), expectedUser.getLastName());
        assertEquals(user.getRole(), expectedUser.getRole());
        assertEquals(user.getCreatedAt(), expectedUser.getCreatedAt());
    }

    @Test
    public void testUser_DatabaseException() {
        //setup
        String userId = "user-id";

        doThrow(new DatabaseException(new Exception("test"))).when(userServiceMock).findById(userId);

        //test
        ExecutionResult result = dgsQueryExecutor.execute(String.format("{ user(userId: \"%s\") { userId firstName lastName role createdAt }}", userId));

        //verify
        assertNotNull(result);
        assertNotNull(result.getErrors());
        assertEquals(result.getErrors().size(), 1);
        assertTrue(result.getErrors().get(0).getMessage().startsWith(Integer.toString(ErrorEnum.DatabaseError.getErrorCode())));
    }
}