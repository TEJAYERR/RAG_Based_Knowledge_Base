package com.ai.knowledge_base.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionController {

    @ExceptionHandler
    public ResponseEntity<?> contentMissingException(
            ContentMissingException contentMissingException,
            HttpServletRequest httpRequest) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .localDateTime(LocalDateTime.now())
                .message(contentMissingException.getMessage())
                .path(httpRequest.getServletPath())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<?> unSupportedContentTypeException(
            UnSupportedContentTypeException unSupportedContentTypeException,
            HttpServletRequest httpRequest) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .localDateTime(LocalDateTime.now())
                .message(unSupportedContentTypeException.getMessage())
                .path(httpRequest.getServletPath())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<?> documentProcessingException(
            DocumentProcessingException documentProcessingException,
            HttpServletRequest httpRequest) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .localDateTime(LocalDateTime.now())
                .message(documentProcessingException.getMessage())
                .path(httpRequest.getServletPath())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<?> retrievalException (
            RetrievalException retrievalException,
            HttpServletRequest httpRequest) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .localDateTime(LocalDateTime.now())
                .message(retrievalException.getMessage())
                .path(httpRequest.getServletPath())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<?> transientAiException (
            TransientAiException transientAiException,
            HttpServletRequest httpRequest) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .localDateTime(LocalDateTime.now())
                .message("AI service is currently unavailable. Please try again later.")
                .path(httpRequest.getServletPath())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<?> nonTransientAiException (
            NonTransientAiException nonTransientAiException,
            HttpServletRequest httpRequest) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .localDateTime(LocalDateTime.now())
                .message("AI service is currently unavailable. Please try again later.")
                .path(httpRequest.getServletPath())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<?> runtimeException (
            RuntimeException runtimeException,
            HttpServletRequest httpRequest) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .localDateTime(LocalDateTime.now())
                .path(httpRequest.getServletPath())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }
}
