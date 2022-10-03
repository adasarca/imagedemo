package com.adasarca.imagedemo.service;

import com.adasarca.imagedemo.model.database.UserRecord;
import com.adasarca.imagedemo.model.domain.User;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.repository.UserRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class UserServiceTest {

    private UserRepository userRepositoryMock;

    private UserService userService;

    @BeforeMethod
    public void setup() {
        this.userRepositoryMock = mock(UserRepository.class);
        this.userService = new UserService(this.userRepositoryMock);
    }

    @Test
    public void testFindById() {
        //setup
        String userId = "user-id";

        UserRecord record = new UserRecord();
        record.setId(userId);
        record.setFirstName("User");
        record.setLastName("Test");
        record.setRoleId(RoleEnum.User.getId());
        record.setCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")));

        doReturn(record).when(userRepositoryMock).findById(userId);

        //test
        User user = this.userService.findById(userId);

        //verify
        assertNotNull(user);
        assertEquals(user.getUserId(), record.getId());
        assertEquals(user.getFirstName(), record.getFirstName());
        assertEquals(user.getLastName(), record.getLastName());
        assertNotNull(user.getRole());
        assertEquals(user.getRole().getId(), record.getRoleId());
        assertEquals(user.getCreatedAt(), record.getCreatedAt());
    }

    @Test
    public void testFindById_NotFound() {
        //setup
        String userId = "user-id";

        doReturn(null).when(userRepositoryMock).findById(userId);

        //test
        User user = this.userService.findById(userId);

        //verify
        assertNull(user);
    }
}