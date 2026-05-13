package com.talentcircle.adapter.out.linkedin;

/**
 * Se lanza cuando LinkedIn responde con HTTP 401 (token expirado o inválido).
 * El GlobalExceptionHandler la mapea a HTTP 502 con mensaje descriptivo.
 */
public class LinkedInTokenExpiredException extends RuntimeException {

    public LinkedInTokenExpiredException() {
        super("El token de LinkedIn ha expirado o es inválido. " +
              "Actualiza LINKEDIN_ACCESS_TOKEN con un token OAuth 2.0 válido.");
    }

    public LinkedInTokenExpiredException(String detail) {
        super("Token de LinkedIn inválido: " + detail);
    }
}
