package com.adasarca.imagedemo.model.enumeration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.GrantedAuthority;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum RoleEnum implements GrantedAuthority {
    Admin(1, Authority.ADMIN),
    User(2, Authority.USER)
    ;

    private final Integer id;
    private final String authority;

    RoleEnum(Integer id, String authority) {
        this.id = id;
        this.authority = authority;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    public static RoleEnum getById(Integer id) {
        for (RoleEnum roleEnum : values())
            if (roleEnum.id.equals(id))
                return roleEnum;
        return null;
    }

    public static class Authority {
        public static final String ADMIN = "ROLE_ADMIN";
        public static final String USER = "ROLE_USER";
    }
}
