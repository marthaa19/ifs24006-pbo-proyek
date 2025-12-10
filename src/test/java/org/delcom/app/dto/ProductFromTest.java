package org.delcom.app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ProductFormTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        // Setup validator manual untuk mengetes anotasi @NotBlank, @Min, dll.
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("✅ Valid: Form dengan data benar harus lolos validasi")
    void testProductForm_Valid() {
        ProductForm form = new ProductForm();
        form.setName("Mouse Wireless");
        form.setSku("MS-001");
        form.setCategory("Aksesoris");
        form.setPrice(new BigDecimal("150000"));
        form.setStock(10);
        form.setDescription("Mouse bagus");

        // Validasi
        Set<ConstraintViolation<ProductForm>> violations = validator.validate(form);

        // Harusnya tidak ada error
        assertTrue(violations.isEmpty(), "Form valid seharusnya tidak memiliki error validasi");
    }

    @Test
    @DisplayName("❌ Invalid: Nama, SKU, dan Kategori kosong harus gagal")
    void testProductForm_InvalidEmptyFields() {
        ProductForm form = new ProductForm();
        // Sengaja dikosongkan (null/empty)
        form.setName("");
        form.setSku("");
        form.setCategory(null);
        form.setPrice(new BigDecimal("10000"));
        form.setStock(5);

        Set<ConstraintViolation<ProductForm>> violations = validator.validate(form);

        // Harusnya ada 3 error (Name, SKU, Category)
        assertFalse(violations.isEmpty());
        assertEquals(3, violations.size());
        
        // Cek pesan error (opsional, pastikan message di DTO sesuai)
        boolean hasNameError = violations.stream().anyMatch(v -> v.getMessage().contains("Nama barang harus diisi"));
        assertTrue(hasNameError);
    }

    @Test
    @DisplayName("❌ Invalid: Harga dan Stok negatif harus gagal")
    void testProductForm_InvalidNumbers() {
        ProductForm form = new ProductForm();
        form.setName("Barang Test");
        form.setSku("TEST-01");
        form.setCategory("Umum");
        
        // Harga < 1 dan Stok < 0
        form.setPrice(new BigDecimal("0")); // Error: Min 1
        form.setStock(-5);                  // Error: Min 0

        Set<ConstraintViolation<ProductForm>> violations = validator.validate(form);

        assertEquals(2, violations.size());
    }

    @Test
    @DisplayName("✅ Test Getter & Setter Image")
    void testImageField() {
        ProductForm form = new ProductForm();
        MultipartFile mockFile = mock(MultipartFile.class);
        
        form.setImage(mockFile);
        
        assertNotNull(form.getImage());
        assertEquals(mockFile, form.getImage());
    }
}