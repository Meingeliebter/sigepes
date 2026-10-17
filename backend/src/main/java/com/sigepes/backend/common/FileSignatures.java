package com.sigepes.backend.common;

/**
 * Tipo real de un archivo por su firma (magic bytes), no por la extensión ni por el
 * {@code Content-Type} que declara el cliente: un ejecutable renombrado a {@code .pdf} no pasa.
 */
public final class FileSignatures {

    public static final String PDF = "application/pdf";
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";

    private FileSignatures() {
    }

    /** Devuelve PDF, JPEG o PNG según la firma, o null si no es ninguno de ellos. */
    public static String detect(byte[] bytes) {
        if (bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F'
                && bytes[4] == '-') {
            return PDF;
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return PNG;
        }
        return null;
    }
}
