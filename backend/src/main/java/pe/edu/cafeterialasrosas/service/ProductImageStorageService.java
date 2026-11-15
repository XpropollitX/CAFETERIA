package pe.edu.cafeterialasrosas.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.cafeterialasrosas.exception.BusinessRuleException;

@Service
public class ProductImageStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ProductImageStorageService.class);
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final String PUBLIC_PATH = "/uploads/product-images/";
    private final Path imageDirectory;

    public ProductImageStorageService(
        @Value("${app.upload-dir:uploads/product-images}") String uploadDirectory
    ) {
        imageDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessRuleException("Selecciona una imagen para el producto.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessRuleException("La imagen no puede superar los 5 MB.");
        }

        byte[] content;
        try {
            content = image.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer la imagen del producto.", exception);
        }
        String extension = detectarExtension(content);
        String fileName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(imageDirectory);
            Files.write(
                imageDirectory.resolve(fileName),
                content,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo guardar la imagen del producto.", exception);
        }
        return PUBLIC_PATH + fileName;
    }

    public void eliminarSiLocal(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(PUBLIC_PATH)) {
            return;
        }
        String fileName = imageUrl.substring(PUBLIC_PATH.length());
        if (!fileName.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp)")) {
            return;
        }
        try {
            Files.deleteIfExists(imageDirectory.resolve(fileName));
        } catch (IOException exception) {
            logger.warn("No se pudo eliminar una imagen local de producto: {}", fileName, exception);
        }
    }

    public String resourceLocation() {
        String location = imageDirectory.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    private String detectarExtension(byte[] content) {
        if (content.length >= 3
            && unsigned(content[0]) == 0xFF
            && unsigned(content[1]) == 0xD8
            && unsigned(content[2]) == 0xFF) {
            return ".jpg";
        }
        if (content.length >= 8
            && unsigned(content[0]) == 0x89
            && content[1] == 0x50
            && content[2] == 0x4E
            && content[3] == 0x47
            && content[4] == 0x0D
            && content[5] == 0x0A
            && content[6] == 0x1A
            && content[7] == 0x0A) {
            return ".png";
        }
        if (content.length >= 12
            && content[0] == 0x52
            && content[1] == 0x49
            && content[2] == 0x46
            && content[3] == 0x46
            && content[8] == 0x57
            && content[9] == 0x45
            && content[10] == 0x42
            && content[11] == 0x50) {
            return ".webp";
        }
        throw new BusinessRuleException("El archivo debe ser una imagen JPG, PNG o WebP válida.");
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
