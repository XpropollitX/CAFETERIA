package pe.edu.cafeterialasrosas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import pe.edu.cafeterialasrosas.exception.BusinessRuleException;

class ProductImageStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesImagesWithGeneratedNamesAndRemovesThemSafely() throws IOException {
        byte[] pngSignature = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        ProductImageStorageService service = new ProductImageStorageService(tempDirectory.toString());

        String imageUrl = service.guardar(
            new MockMultipartFile("imagen", "foto.png", "image/png", pngSignature)
        );
        Path storedFile = tempDirectory.resolve(imageUrl.substring("/uploads/product-images/".length()));

        assertThat(imageUrl).matches("/uploads/product-images/[0-9a-fA-F-]{36}\\.png");
        assertThat(Files.readAllBytes(storedFile)).containsExactly(pngSignature);
        assertThat(service.resourceLocation()).endsWith("/");

        service.eliminarSiLocal(imageUrl);
        assertThat(storedFile).doesNotExist();
    }

    @Test
    void rejectsFilesThatAreNotSupportedImageTypes() {
        ProductImageStorageService service = new ProductImageStorageService(tempDirectory.toString());
        MockMultipartFile invalidImage = new MockMultipartFile(
            "imagen",
            "foto.png",
            "image/png",
            "not an image".getBytes()
        );

        assertThatThrownBy(() -> service.guardar(invalidImage))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("JPG, PNG o WebP");
    }

    @Test
    void rejectsImagesOverFiveMegabytes() {
        ProductImageStorageService service = new ProductImageStorageService(tempDirectory.toString());
        MockMultipartFile oversizedImage = new MockMultipartFile(
            "imagen",
            "foto.png",
            "image/png",
            new byte[5 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> service.guardar(oversizedImage))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("5 MB");
    }
}
