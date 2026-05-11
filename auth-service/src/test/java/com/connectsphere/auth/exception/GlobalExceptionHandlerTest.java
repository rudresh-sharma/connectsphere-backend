package com.connectsphere.auth.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for AuthService GlobalExceptionHandler.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/test");
        when(request.getMethod()).thenReturn("GET");
    }

    @Test
    void handleBadRequestReturns400() {
        ResponseEntity<?> resp = handler.handleBadRequest(new BadRequestException("bad"), request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void handleNotFoundReturns404() {
        ResponseEntity<?> resp = handler.handleNotFound(new ResourceNotFoundException("not found"), request);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void handleBadCredentialsReturns401() {
        ResponseEntity<?> resp = handler.handleBadCredentials(new BadCredentialsException("bad creds"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void handleUsernameNotFoundReturns401() {
        ResponseEntity<?> resp = handler.handleUsernameNotFound(new UsernameNotFoundException("user"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void handleResponseStatusPassesThroughStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "conflict reason");
        ResponseEntity<?> resp = handler.handleResponseStatus(ex, request);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    void handleResponseStatusWithNullReasonUsesStatusString() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        ResponseEntity<?> resp = handler.handleResponseStatus(ex, request);
        assertEquals(HttpStatus.BAD_GATEWAY, resp.getStatusCode());
    }

    @Test
    void handleValidationReturns400WithFieldErrors() {
        FieldError fieldError = new FieldError("obj", "username", "must not be blank");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<?> resp = handler.handleValidation(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void handleUnreadableBodyReturns400() {
        var ex = mock(org.springframework.http.converter.HttpMessageNotReadableException.class);
        ResponseEntity<?> resp = handler.handleUnreadableBody(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void handleIllegalStateReturns502() {
        ResponseEntity<?> resp = handler.handleIllegalState(new IllegalStateException("bad state"), request);
        assertEquals(HttpStatus.BAD_GATEWAY, resp.getStatusCode());
    }

    @Test
    void handleGenericReturns500() {
        ResponseEntity<?> resp = handler.handleGeneric(new RuntimeException("unexpected"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }
}
