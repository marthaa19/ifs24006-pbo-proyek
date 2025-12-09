package org.delcom.app.controllers;

import org.delcom.app.context.AuthContext; // <-- pastikan package ini benar. Kalau berbeda: sesuaikan dengan package AuthContext di projectmu
import org.delcom.app.dto.ProductForm;
import org.delcom.app.entities.Product;
import org.delcom.app.entities.User;
import org.delcom.app.services.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/shop/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ============================
    // LIST PRODUCT
    // ============================
    @GetMapping
    public String listProducts(Model model) {

        User currentUser = AuthContext.getUser(); // <-- ambil user login
        model.addAttribute("products", productService.getAllProductsByUser(currentUser));

        return "pages/shop/products";
    }

    // ============================
    // FORM TAMBAH
    // ============================
    @GetMapping("/add")
    public String addProductForm(Model model) {

        model.addAttribute("form", new ProductForm());

        return "pages/shop/form"; // reuse form.html
    }

    @PostMapping("/add")
    public String saveProduct(@ModelAttribute ProductForm form) throws IOException {

        User currentUser = AuthContext.getUser();
        productService.saveProduct(form, currentUser);

        return "redirect:/shop/products";
    }

    // ============================
    // FORM EDIT
    // ============================
    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable UUID id, Model model) {

        Product product = productService.getProductById(id);

        if (product == null) {
            return "redirect:/shop/products";
        }

        // ====== Safe mapping: isi ProductForm field-by-field (jika tidak ada konstruktor khusus) ======
        ProductForm form = new ProductForm();
        // Pastikan ProductForm punya setter untuk field berikut; sesuaikan bila berbeda
        form.setSku(product.getSku());
        form.setCategory(product.getCategory());
        form.setName(product.getName());
        form.setPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
        form.setStock(product.getStock() != null ? product.getStock() : 0);
        form.setDescription(product.getDescription());
        // NOTE: jangan set image MultipartFile di sini — biasanya form menampilkan url image terpisah

        model.addAttribute("form", form);
        model.addAttribute("product", product);

        return "pages/shop/form";
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(@PathVariable UUID id,
                                @ModelAttribute ProductForm form) throws IOException {

        productService.updateProduct(id, form);

        return "redirect:/shop/products";
    }

    // ============================
    // DELETE PRODUCT
    // ============================
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable UUID id) {

        productService.deleteProduct(id);

        return "redirect:/shop/products";
    }

    // ============================
    // DETAIL PRODUCT
    // ============================
    @GetMapping("/detail/{id}")
    public String detailProduct(@PathVariable UUID id, Model model) {

        Product product = productService.getProductById(id);

        if (product == null) {
            return "redirect:/shop/products";
        }

        model.addAttribute("product", product);

        return "pages/shop/detail";
    }
}
