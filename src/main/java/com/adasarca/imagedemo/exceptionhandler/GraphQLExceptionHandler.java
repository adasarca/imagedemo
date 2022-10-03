package com.adasarca.imagedemo.exceptionhandler;

import com.adasarca.imagedemo.model.enumeration.ErrorEnum;
import com.adasarca.imagedemo.model.exception.AmazonS3Exception;
import com.adasarca.imagedemo.model.exception.DatabaseException;
import com.adasarca.imagedemo.model.exception.ValidationException;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLExceptionHandler.class);

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(DataFetcherExceptionHandlerParameters handlerParameters) {
        DataFetcherExceptionHandlerResult result = onException(handlerParameters);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public DataFetcherExceptionHandlerResult onException(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        GraphQLError graphqlError;

        if (exception instanceof ValidationException) {
            graphqlError = TypedGraphQLError.newBadRequestBuilder()
                    .message(exception.getMessage())
                    .path(handlerParameters.getPath())
                    .build();
        } else if (exception instanceof AccessDeniedException) {
            graphqlError = TypedGraphQLError.newPermissionDeniedBuilder()
                    .message(ErrorEnum.Forbidden.getErrorCode() + " - " + ErrorEnum.Forbidden.getMessage())
                    .path(handlerParameters.getPath())
                    .build();
        } else if (exception instanceof DatabaseException) {
            LOGGER.error("Caught DatabaseException: ", exception);
            graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message(ErrorEnum.DatabaseError.getErrorCode() + " - " + ErrorEnum.DatabaseError.getMessage())
                    .path(handlerParameters.getPath())
                    .build();
        } else if (exception instanceof AmazonS3Exception) {
            LOGGER.error("Caught AmazonS3Exception: ", exception);
            graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message(ErrorEnum.AmazonS3Error.getErrorCode() + " - " + ErrorEnum.AmazonS3Error.getMessage())
                    .path(handlerParameters.getPath())
                    .build();
        } else {
            LOGGER.error("Caught unexpected exception: ", exception);
            graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message(ErrorEnum.InternalServerError.getErrorCode() + " - " + ErrorEnum.InternalServerError.getMessage())
                    .path(handlerParameters.getPath())
                    .build();
        }

        return DataFetcherExceptionHandlerResult.newResult()
                .error(graphqlError)
                .build();
    }
}
