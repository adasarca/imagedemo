package com.adasarca.imagedemo.service;

import com.adasarca.imagedemo.model.database.UserRecord;
import com.adasarca.imagedemo.model.domain.User;
import com.adasarca.imagedemo.model.enumeration.RoleEnum;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findById(String userId) throws DatabaseException {
        UserRecord record = this.userRepository.findById(userId);
        return this.buildUser(record);
    }

    private User buildUser(UserRecord record) {
        if (null == record)
            return null;

        User user = new User();
        user.setUserId(record.getId());
        user.setFirstName(record.getFirstName());
        user.setLastName(record.getLastName());
        user.setRole(RoleEnum.getById(record.getRoleId()));
        user.setCreatedAt(record.getCreatedAt());
        return user;
    }
}
