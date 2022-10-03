package com.adasarca.imagedemo.controller;

import com.adasarca.imagedemo.model.domain.ErrorResponse;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class ErrorController extends AbstractErrorController {

    private static final String STATUS_ATTR = "status";
    private static final String ERROR_ATTR = "error";
    private static final String MESSAGE_ATTR = "message";

    public ErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }

    @RequestMapping("/error")
    public ErrorResponse error(HttpServletRequest request) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE, ErrorAttributeOptions.Include.BINDING_ERRORS));
        Object status = errorAttributes.get(STATUS_ATTR);
        Object error = errorAttributes.get(ERROR_ATTR);
        Object message = errorAttributes.get(MESSAGE_ATTR);

        ErrorResponse errorResponse = new ErrorResponse();
        if (status instanceof Integer) {
            errorResponse.setHttpCode((int)status);
            errorResponse.setErrorCode((int)status);
        }
        if (error instanceof String)
            errorResponse.setMessage((String) error);
        if (message instanceof String)
            errorResponse.setDetails((String)message);

        return errorResponse;
    }
}
