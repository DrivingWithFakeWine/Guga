package pl.piomin.services.guga.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pl.piomin.services.guga.common.DuplicateResourceException;
import pl.piomin.services.guga.common.ForbiddenOperationException;
import pl.piomin.services.guga.common.ResourceNotFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isBlank() ? "参数校验失败" : message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "请求体格式错误", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "参数类型错误", req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler({DuplicateResourceException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleDuplicate(Exception ex, HttpServletRequest req) {
        String message = ex instanceof DuplicateResourceException d ? d.getMessage() : "数据冲突，请检查后重试";
        return build(HttpStatus.CONFLICT, message, req);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenOperationException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("未处理异常: {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误", req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, req.getRequestURI()));
    }
}
