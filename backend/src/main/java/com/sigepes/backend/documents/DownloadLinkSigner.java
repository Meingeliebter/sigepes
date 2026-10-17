package com.sigepes.backend.documents;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AC-15.5 · Enlaces de descarga de corta duración.
 *
 * <p>El enlace lleva la versión, el usuario autorizado y el vencimiento, firmados con HMAC-SHA256.
 * Así la descarga funciona en un {@code <a href>} (sin cabecera de token) sin abrir el archivo a
 * nadie más: alterar cualquier campo invalida la firma y el enlace caduca a los pocos minutos.
 */
@Component
class DownloadLinkSigner {

    static final Duration TTL = Duration.ofMinutes(5);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] key;

    DownloadLinkSigner(@Value("${sigepes.security.jwt-secret}") String secret) {
        // Clave derivada para este propósito: un token de descarga nunca sirve como JWT ni al revés.
        this.key = hmac(secret.getBytes(StandardCharsets.UTF_8), "sigepes-document-download".getBytes(StandardCharsets.UTF_8));
    }

    record Grant(String versionId, String userId, Instant expiresAt) {
    }

    String sign(String versionId, String userId, Instant expiresAt) {
        String payload = versionId + "|" + userId + "|" + expiresAt.getEpochSecond();
        byte[] signature = hmac(key, payload.getBytes(StandardCharsets.UTF_8));
        return ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + ENCODER.encodeToString(signature);
    }

    /** Devuelve el permiso si la firma es válida y no ha caducado; null en cualquier otro caso. */
    Grant verify(String token) {
        try {
            int dot = token.indexOf('.');
            if (dot < 0) {
                return null;
            }
            byte[] payload = DECODER.decode(token.substring(0, dot));
            byte[] signature = DECODER.decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(signature, hmac(key, payload))) {
                return null;
            }
            String[] parts = new String(payload, StandardCharsets.UTF_8).split("\\|");
            Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(parts[2]));
            return expiresAt.isAfter(Instant.now()) ? new Grant(parts[0], parts[1], expiresAt) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static byte[] hmac(byte[] secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 no disponible", e);
        }
    }
}
