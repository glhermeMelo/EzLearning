package com.ezlearning.controller;

import com.ezlearning.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        var response = new ErrorResponse(400, "Bad Request", ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex) {
        var response = new ErrorResponse(400, "Bad Request", "Arquivo excede o limite de 5MB", LocalDateTime.now());
        return ResponseEntity.badRequest().body(response);
    }
}
