package org.delcom.app.services;

import org.delcom.app.dto.ProductForm;
import org.delcom.app.entities.Product;
import org.delcom.app.entities.User;
import org.delcom.app.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public ProductService(ProductRepository productRepository, FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Product> getAllProductsByUser(User user) {
        return productRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id).orElse(null);
    }

@Transactional
    public void saveProduct(ProductForm form, User user) throws IOException {
        Product product = new Product();
        
        // ❌ HAPUS ATAU KOMENTARI BARIS INI:
        // product.setId(UUID.randomUUID()); 
        
        // ✅ BIARKAN KOSONG, GenerateValue di Entity akan mengisinya otomatis
        
        product.setUserId(user.getId());
        product.setSku(form.getSku());
        product.setCategory(form.getCategory());
        
        product.setName(form.getName());
        product.setPrice(form.getPrice());
        product.setStock(form.getStock());
        product.setDescription(form.getDescription());

        // Untuk nama file gambar, kita butuh ID.
        // Triknya: Simpan dulu untuk dapat ID, baru update gambar, atau generate manual tapi jangan pakai @GeneratedValue.
        // TAPI solusi paling aman dan mudah di JPA: Generate UUID manual HANYA untuk nama file, tapi jangan set ke object product sebelum di-save jika pakai @GeneratedValue.
        
        // SOLUSI TERBAIK AGAR TIDAK ERROR:
        // Kita biarkan save dulu untuk generate ID, baru simpan gambar.
        
        // 1. Simpan data dasar dulu (Tanpa gambar & ID)
        Product savedProduct = productRepository.save(product); 
        
        // 2. Sekarang savedProduct sudah punya ID otomatis dari database
        if (form.getImage() != null && !form.getImage().isEmpty()) {
            String fileName = fileStorageService.storeFile(form.getImage(), savedProduct.getId());
            savedProduct.setImageUrl(fileName);
            
            // 3. Update lagi untuk simpan url gambar
            productRepository.save(savedProduct); 
        }
    }

    @Transactional
    public void updateProduct(UUID id, ProductForm form) throws IOException {
        Product product = getProductById(id);
        if (product != null) {
            product.setName(form.getName());
            product.setPrice(form.getPrice());
            product.setStock(form.getStock());
            product.setDescription(form.getDescription());

            // Cek apakah user mengupload gambar baru
            if (form.getImage() != null && !form.getImage().isEmpty()) {
                // Hapus file lama jika ada
                if (product.getImageUrl() != null) {
                    fileStorageService.deleteFile(product.getImageUrl());
                }
                // Simpan file baru
                String fileName = fileStorageService.storeFile(form.getImage(), product.getId());
                product.setImageUrl(fileName);
            }
            
            productRepository.save(product);
        }
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        if (product != null) {
            if (product.getImageUrl() != null) {
                fileStorageService.deleteFile(product.getImageUrl());
            }
            productRepository.delete(product);
        }
    }
}