package com.warehouse.system.Service.File;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    public record StoredFile(String storedName, String contentType) {}
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final Tika tika = new Tika();
    private final Path root;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public StoredFile store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String detectedType = detectContentType(file);
        String storedName = UUID.randomUUID() + extensionFor(detectedType);
        Path target = resolveSafe(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        return new StoredFile(storedName, detectedType);
    }

    public Resource load(String storedName) throws IOException {
        Path file = resolveSafe(storedName);
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("File not found: " + storedName);
        }
        return resource;
    }

    public void delete(String storedName) throws IOException {
        Files.deleteIfExists(resolveSafe(storedName));
    }

    private Path resolveSafe(String name) {
        Path resolved = root.resolve(name).normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("Invalid file path");
        }
        return resolved;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private String detectContentType(MultipartFile file) throws IOException {
        String detected;
        try (InputStream in = file.getInputStream()) {
            detected = tika.detect(in);
        }
        if (!ALLOWED_TYPES.contains(detected)) {
            throw new IllegalArgumentException(
                    "Unsupported or invalid image file (allowed: JPEG, PNG, WebP)");
        }
        return detected;
    }

}
