package com.nexvia.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

    @Test
    void handleNotFound_returns404() {
        var response = handler.handleNotFound(new ResourceNotFoundException("No existe"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("No existe");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleDuplicate_returns409() {
        var response = handler.handleDuplicate(new DuplicateResourceException("Duplicado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Duplicado");
    }

    @Test
    void handleIllegalArgument_returns400() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("Inválido"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Inválido");
    }

    @Test
    void handleBadCredentials_returns401() {
        var response = handler.handleBadCredentials(new BadCredentialsException("bad"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Email o contraseña incorrectos");
    }

    @Test
    void handleForbidden_returns403() {
        var response = handler.handleForbidden(new ForbiddenException("Sin permiso"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Sin permiso");
    }

    @Test
    void apiError_hasAllFields() {
        var response = handler.handleNotFound(new ResourceNotFoundException("test"), request);
        var body = response.getBody();

        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("Not Found");
    }
}
