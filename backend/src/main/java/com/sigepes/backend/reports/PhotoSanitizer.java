package com.sigepes.backend.reports;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;

/**
 * AC-10.5 · Limpia las fotos de una denuncia antes de guardarlas.
 *
 * <p>El tipo se decide por la firma real del archivo, no por la extensión ni por el
 * {@code Content-Type} que declare el cliente. Luego la imagen se decodifica y se vuelve a
 * codificar: el archivo resultante solo contiene píxeles, así que el EXIF (GPS, modelo del
 * teléfono, fecha) y cualquier otro bloque de metadatos desaparecen.
 */
@Component
public class PhotoSanitizer {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    /** Evita "bombas de descompresión": imágenes pequeñas en disco que ocupan gigas en memoria. */
    private static final long MAX_PIXELS = 40_000_000L;

    public record CleanPhoto(byte[] content, String contentType) {
    }

    public CleanPhoto sanitize(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.of(ErrorCode.INVALID_REPORT_PHOTO, "Cada foto puede pesar como máximo 5 MB.");
        }
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw ApiException.of(ErrorCode.INVALID_REPORT_PHOTO);
        }
        String format = detectFormat(original);
        if (format == null) {
            throw ApiException.of(ErrorCode.INVALID_REPORT_PHOTO, "Las fotos deben ser imágenes JPG o PNG.");
        }

        BufferedImage image = decode(original);
        if (format.equals("jpg") && image.getColorModel().hasAlpha()) {
            image = withoutAlpha(image);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(original.length);
        try {
            if (!ImageIO.write(image, format, out)) {
                throw ApiException.of(ErrorCode.INVALID_REPORT_PHOTO);
            }
        } catch (IOException e) {
            throw ApiException.of(ErrorCode.INVALID_REPORT_PHOTO);
        }
        return new CleanPhoto(out.toByteArray(), format.equals("jpg") ? "image/jpeg" : "image/png");
    }

    /** Firma (magic bytes) de JPEG y PNG. */
    static String detectFormat(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "png";
        }
        return null;
    }

    private static BufferedImage decode(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw ApiException.of(ErrorCode.INVALID_REPORT_PHOTO);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw ApiException.of(ErrorCode.INVALID_REPORT_PHOTO, "La resolución de la foto es demasiado alta.");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.of(ErrorCode.INVALID_REPORT_PHOTO, "La foto está dañada o no es una imagen válida.");
        }
    }

    private static BufferedImage withoutAlpha(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, java.awt.Color.WHITE, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }
}
