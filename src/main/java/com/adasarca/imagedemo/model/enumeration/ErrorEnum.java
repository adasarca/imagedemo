package com.adasarca.imagedemo.model.enumeration;

import org.springframework.http.HttpStatus;

public enum ErrorEnum {
    BadRequest(400, HttpStatus.BAD_REQUEST, "Bad Request"),
    Forbidden(403, HttpStatus.FORBIDDEN, "Forbidden"),
    ValidationError(460, HttpStatus.BAD_REQUEST, "Validation Error"),

    InternalServerError(500, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
    DatabaseError(530, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
    AmazonS3Error(540, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error")
    ;

    private final int errorCode;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorEnum(int errorCode, HttpStatus httpStatus, String message) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
