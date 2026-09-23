package com.sigepes.backend.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Almacenamiento de archivos subidos por el usuario (adjuntos de permisos, AC-08.5).
 *
 * <p>Guarda en un directorio local del servidor, no en un bucket externo — suficiente para el MVP.
 * En un despliegue real en Docker, {@code sigepes.storage.uploads-dir} debe apuntar a un volumen
 * persistente; sin uno, los adjuntos se pierden al recrear el contenedor. Esa es una decisión de
 * infraestructura de despliegue, fuera del alcance de este código.
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");

    private final Path root;
    private final long maxFileSizeBytes;

    public FileStorageService(
            @Value("${sigepes.storage.uploads-dir:uploads}") String uploadsDir,
            @Value("${sigepes.storage.max-file-size-mb:8}") long maxFileSizeMb) {
        this.root = Path.of(uploadsDir).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
    }

    public record StoredFile(String storagePath, String originalFilename, String contentType, long sizeBytes) {
    }

    /**
     * Valida y guarda un archivo bajo {@code subdir}, con el nombre físico igual a {@code id} (nunca
     * el nombre original del cliente, para evitar path traversal / colisiones).
     */
    public StoredFile store(MultipartFile file, String subdir, String id) {
        if (file == null || file.isEmpty()) {
            throw ApiException.of(ErrorCode.PERMIT_ATTACHMENTS_REQUIRED);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "El archivo debe ser PDF, JPG o PNG (recibido: " + contentType + ").");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "El archivo supera el tamaño máximo permitido (" + (maxFileSizeBytes / (1024 * 1024)) + " MB).");
        }

        try {
            Path dir = root.resolve(subdir).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(id);
            file.transferTo(target);
            String relative = subdir + "/" + id;
            String originalFilename = file.getOriginalFilename() == null ? id : file.getOriginalFilename();
            return new StoredFile(relative, originalFilename, contentType, file.getSize());
        } catch (IOException e) {
            throw ApiException.of(ErrorCode.INTERNAL_ERROR, "No fue posible guardar el archivo.");
        }
    }

    public Resource load(String storagePath) {
        Path file = root.resolve(storagePath).normalize();
        if (!file.startsWith(root) || !Files.exists(file)) {
            throw ApiException.of(ErrorCode.PERMIT_ATTACHMENT_NOT_FOUND);
        }
        return new FileSystemResource(file);
    }
}
