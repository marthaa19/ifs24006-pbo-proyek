package org.delcom.app.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("✅ Test Getter & Setter Utama")
    void testProductSettersAndGetters() {
        Product product = new Product();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("500000");

        product.setId(id);
        product.setUserId(userId);
        product.setName("Headset Gaming");
        product.setSku("HS-999");
        product.setCategory("Audio");
        product.setPrice(price);
        product.setStock(25);
        product.setDescription("Suara jernih");
        product.setImageUrl("image_123.jpg");

        // Assert
        assertEquals(id, product.getId());
        assertEquals(userId, product.getUserId());
        assertEquals("Headset Gaming", product.getName());
        assertEquals("HS-999", product.getSku());
        assertEquals("Audio", product.getCategory());
        assertEquals(price, product.getPrice());
        assertEquals(25, product.getStock());
        assertEquals("Suara jernih", product.getDescription());
        assertEquals("image_123.jpg", product.getImageUrl());
    }

    @Test
    @DisplayName("✅ Test Lifecycle @PrePersist (onCreate)")
    void testOnCreate() {
        Product product = new Product();
        
        // Pastikan awal null
        assertNull(product.getCreatedAt());
        assertNull(product.getUpdatedAt());

        // Panggil method protected onCreate() secara manual
        // Ini bisa dilakukan karena ProductTest ada di package yg sama dengan Product
        product.onCreate(); 

        // Assert bahwa tanggal terisi
        assertNotNull(product.getCreatedAt(), "CreatedAt harus terisi setelah onCreate");
        assertNotNull(product.getUpdatedAt(), "UpdatedAt harus terisi setelah onCreate");
        
        // Pastikan waktu creation tidak di masa depan (sanity check)
        assertTrue(product.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("✅ Test Lifecycle @PreUpdate (onUpdate)")
    void testOnUpdate() throws InterruptedException {
        Product product = new Product();
        product.onCreate(); // Init awal
        
        LocalDateTime createdTime = product.getCreatedAt();
        LocalDateTime initialUpdatedTime = product.getUpdatedAt();

        // Beri jeda sedikit agar waktu berubah (10ms)
        Thread.sleep(10);

        // Panggil onUpdate
        product.onUpdate();

        // Assert
        assertEquals(createdTime, product.getCreatedAt(), "CreatedAt tidak boleh berubah saat update");
        assertTrue(product.getUpdatedAt().isAfter(initialUpdatedTime), "UpdatedAt harus lebih baru dari waktu sebelumnya");
    }
}