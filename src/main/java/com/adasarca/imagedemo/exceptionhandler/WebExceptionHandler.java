package com.adasarca.imagedemo.exceptionhandler;

import com.adasarca.imagedemo.model.domain.ErrorResponse;
import com.adasarca.imagedemo.model.enumeration.ErrorEnum;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.AmazonS3Exception;
import com.adasarca.imagedemo.model.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public final ResponseEntity<ErrorResponse> handleDaoValidationException(Exception exception) {
        LOGGER.error("Caught ValidationException: ", exception);

        ErrorResponse errorResponse = new ErrorResponse(ErrorEnum.ValidationError, exception.getMessage());
        return new ResponseEntity<>(errorResponse, ErrorEnum.ValidationError.getHttpStatus());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public final ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(Exception exception) {
        LOGGER.error("Caught MaxUploadSizeExceededException: ", exception);

        ErrorResponse errorResponse = new ErrorResponse(ErrorEnum.ValidationError, "Maximum upload file exceeded");
        return new ResponseEntity<>(errorResponse, ErrorEnum.ValidationError.getHttpStatus());
    }

    @ExceptionHandler(DatabaseException.class)
    public final ResponseEntity<ErrorResponse> handleDatabaseException(Exception exception) {
        LOGGER.error("Caught DatabaseException: ", exception);

        ErrorResponse errorResponse = new ErrorResponse(ErrorEnum.DatabaseError);
        return new ResponseEntity<>(errorResponse, ErrorEnum.DatabaseError.getHttpStatus());
    }

    @ExceptionHandler(AmazonS3Exception.class)
    public final ResponseEntity<ErrorResponse> handleAmazonS3Exception(Exception exception) {
        LOGGER.error("Caught AmazonS3Exception: ", exception);

        ErrorResponse errorResponse = new ErrorResponse(ErrorEnum.AmazonS3Error);
        return new ResponseEntity<>(errorResponse, ErrorEnum.AmazonS3Error.getHttpStatus());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public final ResponseEntity<ErrorResponse> handleResponseStatusException(Exception exception) {
        LOGGER.error("Caught ResponseStatusException: ", exception);

        if (exception instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException)exception;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorCode(responseStatusException.getStatus().value());
            errorResponse.setHttpCode(responseStatusException.getStatus().value());
            errorResponse.setMessage(responseStatusException.getStatus().getReasonPhrase());
            errorResponse.setDetails(responseStatusException.getReason());
            return new ResponseEntity<>(errorResponse, responseStatusException.getStatus());
        }

        LOGGER.error("Could not grab ResponseStatusException fields, returning 500 [Internal Server Error]...");
        ErrorResponse errorResponse = new ErrorResponse(ErrorEnum.InternalServerError);
        return new ResponseEntity<>(errorResponse, ErrorEnum.InternalServerError.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        LOGGER.error("Caught unexpected exception: ", exception);

        ErrorResponse errorResponse = new ErrorResponse(ErrorEnum.InternalServerError);
        return new ResponseEntity<>(errorResponse, ErrorEnum.InternalServerError.getHttpStatus());
    }
}
