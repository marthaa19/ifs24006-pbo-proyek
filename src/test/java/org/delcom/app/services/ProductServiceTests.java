package org.delcom.app.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.delcom.app.dto.ProductForm;
import org.delcom.app.entities.Product;
import org.delcom.app.entities.User;
import org.delcom.app.repositories.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

public class ProductServiceTests {
    @Test
    @DisplayName("Pengujian untuk service Product")
    void testProductService() throws Exception {
        // Buat random UUID
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID nonexistentProductId = UUID.randomUUID();

        // Membuat dummy data
        User authUser = new User("Test User", "testuser@example.com");
        authUser.setId(userId);

        Product product = new Product();
        product.setId(productId);
        product.setUserId(userId);
        product.setName("Laptop Gaming");
        product.setSku("LPT-001");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("15000000"));
        product.setStock(10);
        product.setDescription("Laptop gaming high-end");
        product.setImageUrl("laptop-gaming.jpg");

        ProductForm validForm = new ProductForm();
        validForm.setName("Laptop Gaming");
        validForm.setSku("LPT-001");
        validForm.setCategory("Electronics");
        validForm.setPrice(new BigDecimal("15000000"));
        validForm.setStock(10);
        validForm.setDescription("Laptop gaming high-end");

        // Membuat mock Repository dan FileStorageService
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);

        // Atur perilaku mock
        when(productRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(product));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.findById(nonexistentProductId)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(fileStorageService.storeFile(any(MultipartFile.class), any(UUID.class))).thenReturn("laptop-gaming.jpg");

        // Membuat instance service
        ProductService productService = new ProductService(productRepository, fileStorageService);
        assert (productService != null);

        // Menguji method getAllProductsByUser
        {
            List<Product> result = productService.getAllProductsByUser(authUser);
            assert (result != null);
            assert (!result.isEmpty());
            assert (result.size() == 1);
            assert (result.get(0).getId().equals(productId));
            verify(productRepository, times(1)).findByUserIdOrderByCreatedAtDesc(userId);
        }

        // Menguji method getProductById
        {
            // Product dengan ID yang ada
            {
                Product result = productService.getProductById(productId);
                assert (result != null);
                assert (result.getId().equals(productId));
                verify(productRepository, times(1)).findById(productId);
            }

            // Product dengan ID yang tidak ada
            {
                Product result = productService.getProductById(nonexistentProductId);
                assert (result == null);
                verify(productRepository, times(1)).findById(nonexistentProductId);
            }
        }

        // Menguji method saveProduct
        {
            // Save product tanpa gambar
            {
                ProductForm formWithoutImage = new ProductForm();
                formWithoutImage.setName("Mouse Gaming");
                formWithoutImage.setSku("MSE-001");
                formWithoutImage.setCategory("Electronics");
                formWithoutImage.setPrice(new BigDecimal("500000"));
                formWithoutImage.setStock(20);
                formWithoutImage.setDescription("Mouse gaming RGB");
                formWithoutImage.setImage(null);

                productService.saveProduct(formWithoutImage, authUser);
                verify(productRepository, times(1)).save(any(Product.class));
                verify(fileStorageService, never()).storeFile(any(MultipartFile.class), any(UUID.class));
            }

            // Save product dengan gambar (image tidak null dan tidak empty)
            {
                when(mockFile.isEmpty()).thenReturn(false);
                validForm.setImage(mockFile);

                productService.saveProduct(validForm, authUser);
                verify(productRepository, times(3)).save(any(Product.class)); // 1 untuk save awal + 1 untuk update image url + 1 dari test sebelumnya
                verify(fileStorageService, times(1)).storeFile(mockFile, productId);
            }

            // Save product dengan gambar empty
            {
                MultipartFile emptyFile = Mockito.mock(MultipartFile.class);
                when(emptyFile.isEmpty()).thenReturn(true);
                
                ProductForm formWithEmptyImage = new ProductForm();
                formWithEmptyImage.setName("Keyboard Gaming");
                formWithEmptyImage.setSku("KBD-001");
                formWithEmptyImage.setCategory("Electronics");
                formWithEmptyImage.setPrice(new BigDecimal("800000"));
                formWithEmptyImage.setStock(15);
                formWithEmptyImage.setDescription("Keyboard mechanical");
                formWithEmptyImage.setImage(emptyFile);

                productService.saveProduct(formWithEmptyImage, authUser);
                verify(productRepository, times(4)).save(any(Product.class));
            }
        }

        // Menguji method updateProduct
        {
            // Update product yang ada tanpa mengubah gambar (image = null)
            {
                ProductForm updateFormWithoutImage = new ProductForm();
                updateFormWithoutImage.setName("Laptop Gaming Updated");
                updateFormWithoutImage.setSku("LPT-001");
                updateFormWithoutImage.setCategory("Electronics");
                updateFormWithoutImage.setPrice(new BigDecimal("16000000"));
                updateFormWithoutImage.setStock(8);
                updateFormWithoutImage.setDescription("Laptop gaming updated");
                updateFormWithoutImage.setImage(null);

                productService.updateProduct(productId, updateFormWithoutImage);
                verify(productRepository, times(5)).save(any(Product.class));
            }

            // Update product tanpa mengubah gambar (image empty)
            {
                MultipartFile emptyFile = Mockito.mock(MultipartFile.class);
                when(emptyFile.isEmpty()).thenReturn(true);
                
                ProductForm updateFormWithEmptyImage = new ProductForm();
                updateFormWithEmptyImage.setName("Laptop Gaming Updated 2");
                updateFormWithEmptyImage.setSku("LPT-001");
                updateFormWithEmptyImage.setCategory("Electronics");
                updateFormWithEmptyImage.setPrice(new BigDecimal("16500000"));
                updateFormWithEmptyImage.setStock(7);
                updateFormWithEmptyImage.setDescription("Laptop gaming updated 2");
                updateFormWithEmptyImage.setImage(emptyFile);

                productService.updateProduct(productId, updateFormWithEmptyImage);
                verify(productRepository, times(6)).save(any(Product.class));
            }

            // Update product dengan gambar baru (hapus gambar lama)
            {
                MultipartFile newMockFile = Mockito.mock(MultipartFile.class);
                when(newMockFile.isEmpty()).thenReturn(false);
                
                ProductForm updateFormWithImage = new ProductForm();
                updateFormWithImage.setName("Laptop Gaming Pro");
                updateFormWithImage.setSku("LPT-001");
                updateFormWithImage.setCategory("Electronics");
                updateFormWithImage.setPrice(new BigDecimal("17000000"));
                updateFormWithImage.setStock(5);
                updateFormWithImage.setDescription("Laptop gaming pro");
                updateFormWithImage.setImage(newMockFile);

                productService.updateProduct(productId, updateFormWithImage);
                verify(fileStorageService, times(1)).deleteFile("laptop-gaming.jpg");
                verify(fileStorageService, times(2)).storeFile(any(MultipartFile.class), any(UUID.class));
                verify(productRepository, times(7)).save(any(Product.class));
            }

            // Update product dengan gambar baru (product tidak punya gambar lama)
            {
                Product productWithoutImage = new Product();
                productWithoutImage.setId(productId);
                productWithoutImage.setUserId(userId);
                productWithoutImage.setName("Monitor Gaming");
                productWithoutImage.setSku("MON-001");
                productWithoutImage.setCategory("Electronics");
                productWithoutImage.setPrice(new BigDecimal("3000000"));
                productWithoutImage.setStock(12);
                productWithoutImage.setDescription("Monitor 144Hz");
                productWithoutImage.setImageUrl(null);

                when(productRepository.findById(productId)).thenReturn(Optional.of(productWithoutImage));

                MultipartFile newImage = Mockito.mock(MultipartFile.class);
                when(newImage.isEmpty()).thenReturn(false);
                
                ProductForm updateForm = new ProductForm();
                updateForm.setName("Monitor Gaming Pro");
                updateForm.setSku("MON-001");
                updateForm.setCategory("Electronics");
                updateForm.setPrice(new BigDecimal("3500000"));
                updateForm.setStock(10);
                updateForm.setDescription("Monitor 240Hz");
                updateForm.setImage(newImage);

                productService.updateProduct(productId, updateForm);
                verify(fileStorageService, times(3)).storeFile(any(MultipartFile.class), any(UUID.class));
                verify(productRepository, times(8)).save(any(Product.class));
            }

            // Update product yang tidak ada
            {
                ProductForm updateForm = new ProductForm();
                updateForm.setName("Non Existent Product");
                updateForm.setSku("XXX-999");
                updateForm.setCategory("Unknown");
                updateForm.setPrice(new BigDecimal("1000000"));
                updateForm.setStock(1);
                updateForm.setDescription("This should not be saved");

                productService.updateProduct(nonexistentProductId, updateForm);
                // Save tidak dipanggil karena product tidak ditemukan
                verify(productRepository, times(8)).save(any(Product.class)); // tetap 8 dari test sebelumnya
            }
        }

        // Menguji method deleteProduct
        {
            // Kembalikan mock product dengan image untuk test delete
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // Delete product yang ada dengan gambar
            {
                productService.deleteProduct(productId);
                verify(fileStorageService, times(2)).deleteFile("laptop-gaming.jpg");
                verify(productRepository, times(1)).delete(product);
            }

            // Delete product yang ada tanpa gambar
            {
                Product productWithoutImage = new Product();
                productWithoutImage.setId(UUID.randomUUID());
                productWithoutImage.setUserId(userId);
                productWithoutImage.setName("Product Without Image");
                productWithoutImage.setImageUrl(null);

                UUID productWithoutImageId = productWithoutImage.getId();
                when(productRepository.findById(productWithoutImageId)).thenReturn(Optional.of(productWithoutImage));

                productService.deleteProduct(productWithoutImageId);
                verify(productRepository, times(2)).delete(any(Product.class));
            }

            // Delete product yang tidak ada
            {
                productService.deleteProduct(nonexistentProductId);
                // Delete tidak dipanggil karena product tidak ditemukan
                verify(productRepository, times(2)).delete(any(Product.class)); // tetap 2 dari test sebelumnya
            }
        }
    }
}