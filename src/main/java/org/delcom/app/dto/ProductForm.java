package org.delcom.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

public class ProductForm {

    // --- TAMBAHAN BARU (SKU & Category) ---
    @NotBlank(message = "Kode Barang (SKU) harus diisi")
    private String sku;

    @NotBlank(message = "Kategori harus dipilih")
    private String category;
    // --------------------------------------

    @NotBlank(message = "Nama barang harus diisi")
    private String name;

    @NotNull(message = "Harga harus diisi")
    @Min(value = 1, message = "Harga minimal 1")
    private BigDecimal price;

    @NotNull(message = "Stok harus diisi")
    @Min(value = 0, message = "Stok tidak boleh minus")
    private Integer stock;

    private String description;

    // File gambar (opsional saat edit)
    private MultipartFile image;

    // ==========================================
    // GETTERS & SETTERS (Pastikan ini ada semua)
    // ==========================================

    // 1. Getter Setter SKU
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    // 2. Getter Setter Category
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // 3. Getter Setter Lainnya
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }
}