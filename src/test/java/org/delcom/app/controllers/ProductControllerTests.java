package org.delcom.app.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.UUID;

import org.delcom.app.dto.ProductForm;
import org.delcom.app.entities.Product;
import org.delcom.app.entities.User;
import org.delcom.app.services.ProductService;
import org.delcom.app.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;

public class ProductControllerTests {
    @Test
    @DisplayName("Pengujian untuk controller Product")
    void testProductController() throws Exception {
        // Buat random UUID
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID nonexistentProductId = UUID.randomUUID();

        // Membuat dummy data
        User authUser = new User("Test User", "testuser@example.com");
        authUser.setId(userId);

        Product product = new Product();
        product.setId(productId);
        product.setName("Laptop Gaming");
        product.setSku("LPT-001");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("15000000"));
        product.setStock(10);
        product.setDescription("Laptop gaming high-end");

        ProductForm validForm = new ProductForm();
        validForm.setName("Laptop Gaming");
        validForm.setSku("LPT-001");
        validForm.setCategory("Electronics");
        validForm.setPrice(new BigDecimal("15000000"));
        validForm.setStock(10);
        validForm.setDescription("Laptop gaming high-end");

        // Membuat mock Service
        ProductService productService = Mockito.mock(ProductService.class);
        UserService userService = Mockito.mock(UserService.class);
        Model model = Mockito.mock(Model.class);
        Principal principal = Mockito.mock(Principal.class);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        RedirectAttributes redirectAttributes = Mockito.mock(RedirectAttributes.class);

        // Atur perilaku mock
        when(principal.getName()).thenReturn("testuser@example.com");
        when(userService.getUserByEmail("testuser@example.com")).thenReturn(authUser);
        when(productService.getAllProductsByUser(any(User.class))).thenReturn(List.of(product));
        doNothing().when(productService).saveProduct(any(ProductForm.class), any(User.class));
        when(productService.getProductById(productId)).thenReturn(product);
        when(productService.getProductById(nonexistentProductId)).thenReturn(null);
        doNothing().when(productService).updateProduct(any(UUID.class), any(ProductForm.class));
        doNothing().when(productService).deleteProduct(any(UUID.class));

        // Membuat instance controller
        ProductController productController = new ProductController(productService, userService);
        assert (productController != null);

        // Menguji method index (tampilan daftar data)
        {
            // User terautentikasi
            {
                String result = productController.index(model, principal);
                assert (result != null);
                assert (result.equals("pages/products/index"));
            }

            // User tidak terautentikasi
            {
                when(userService.getUserByEmail(any())).thenReturn(null);
                String result = productController.index(model, null);
                assert (result != null);
                assert (result.equals("pages/products/index"));
            }
        }

        // Menguji method showCreate (form tambah)
        {
            String result = productController.showCreate(model);
            assert (result != null);
            assert (result.equals("pages/products/create"));
        }

        // Menguji method create (proses tambah)
        {
            // Data tidak valid - dengan error
            {
                when(bindingResult.hasErrors()).thenReturn(true);
                List<ObjectError> errors = new ArrayList<>();
                errors.add(new ObjectError("productForm", "Validation error"));
                when(bindingResult.getAllErrors()).thenReturn(errors);
                
                String result = productController.create(validForm, bindingResult, principal, redirectAttributes);
                assert (result != null);
                assert (result.equals("pages/products/create"));
            }

            // User tidak terautentikasi
            {
                when(bindingResult.hasErrors()).thenReturn(false);
                when(userService.getUserByEmail(any())).thenReturn(null);
                String result = productController.create(validForm, bindingResult, principal, redirectAttributes);
                assert (result != null);
                assert (result.equals("redirect:/auth/logout"));
            }

            // Berhasil menambahkan product
            {
                when(bindingResult.hasErrors()).thenReturn(false);
                when(userService.getUserByEmail("testuser@example.com")).thenReturn(authUser);
                String result = productController.create(validForm, bindingResult, principal, redirectAttributes);
                assert (result != null);
                assert (result.equals("redirect:/products"));
            }

            // IOException saat create
            {
                when(bindingResult.hasErrors()).thenReturn(false);
                when(userService.getUserByEmail("testuser@example.com")).thenReturn(authUser);
                doThrow(new IOException("Upload failed")).when(productService).saveProduct(any(ProductForm.class), any(User.class));
                
                String result = productController.create(validForm, bindingResult, principal, redirectAttributes);
                assert (result != null);
                assert (result.equals("pages/products/create"));
                
                // Reset mock untuk test selanjutnya
                doNothing().when(productService).saveProduct(any(ProductForm.class), any(User.class));
            }

            // Exception lainnya saat create
            {
                when(bindingResult.hasErrors()).thenReturn(false);
                when(userService.getUserByEmail("testuser@example.com")).thenReturn(authUser);
                doThrow(new RuntimeException("Unknown error")).when(productService).saveProduct(any(ProductForm.class), any(User.class));
                
                String result = productController.create(validForm, bindingResult, principal, redirectAttributes);
                assert (result != null);
                assert (result.equals("redirect:/products"));
                
                // Reset mock untuk test selanjutnya
                doNothing().when(productService).saveProduct(any(ProductForm.class), any(User.class));
            }
        }

        // Menguji method detail (tampilan detail)
        {
            // Product dengan ID yang ada
            {
                String result = productController.detail(productId, model);
                assert (result != null);
                assert (result.equals("pages/products/detail"));
            }

            // Product dengan ID yang tidak ada
            {
                String result = productController.detail(nonexistentProductId, model);
                assert (result != null);
                assert (result.equals("redirect:/products"));
            }
        }

        // Menguji method showEdit (form ubah)
        {
            // Product dengan ID yang ada
            {
                String result = productController.showEdit(productId, model);
                assert (result != null);
                assert (result.equals("pages/products/edit"));
            }

            // Product dengan ID yang tidak ada
            {
                String result = productController.showEdit(nonexistentProductId, model);
                assert (result != null);
                assert (result.equals("redirect:/products"));
            }
        }

        // Menguji method update (proses ubah)
        {
            // Data tidak valid
            {
                when(bindingResult.hasErrors()).thenReturn(true);
                String result = productController.update(productId, validForm, bindingResult, redirectAttributes, model);
                assert (result != null);
                assert (result.equals("pages/products/edit"));
            }

            // Berhasil memperbarui product
            {
                when(bindingResult.hasErrors()).thenReturn(false);
                String result = productController.update(productId, validForm, bindingResult, redirectAttributes, model);
                assert (result != null);
                assert (result.equals("redirect:/products"));
            }

            // IOException saat update
            {
                when(bindingResult.hasErrors()).thenReturn(false);
                doThrow(new IOException("Upload failed")).when(productService).updateProduct(any(UUID.class), any(ProductForm.class));
                
                String result = productController.update(productId, validForm, bindingResult, redirectAttributes, model);
                assert (result != null);
                assert (result.equals("pages/products/edit"));
                
                // Reset mock untuk test selanjutnya
                doNothing().when(productService).updateProduct(any(UUID.class), any(ProductForm.class));
            }
        }

        // Menguji method delete (hapus data)
        {
            String result = productController.delete(productId, redirectAttributes);
            assert (result != null);
            assert (result.equals("redirect:/products"));
        }
    }
}