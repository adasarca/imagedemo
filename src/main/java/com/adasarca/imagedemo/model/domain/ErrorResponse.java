package com.adasarca.imagedemo.model.domain;

import com.adasarca.imagedemo.model.enumeration.ErrorEnum;

public class ErrorResponse {

    private int errorCode;
    private int httpCode;
    private String message;
    private String details;

    public ErrorResponse() {}

    public ErrorResponse(ErrorEnum errorEnum) {
        if (null == errorEnum)
            throw new IllegalArgumentException("errorEnum");

        this.errorCode = errorEnum.getErrorCode();
        this.httpCode = errorEnum.getHttpStatus().value();
        this.message = errorEnum.getMessage();
    }

    public ErrorResponse(ErrorEnum errorEnum, String details) {
        this(errorEnum);
        this.details = details;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
