package pl.piomin.services.guga.web;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorResponse(int status, String error, String message, String path, Instant timestamp) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, path, Instant.now());
    }
}
