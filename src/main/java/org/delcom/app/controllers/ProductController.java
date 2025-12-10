package org.delcom.app.controllers;

import jakarta.validation.Valid;
import org.delcom.app.dto.ProductForm;
import org.delcom.app.entities.Product;
import org.delcom.app.entities.User;
import org.delcom.app.services.ProductService;
import org.delcom.app.services.UserService;
import org.delcom.app.utils.ConstUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    public ProductController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) return null;
        return userService.getUserByEmail(principal.getName());
    }

    // 1. Fitur Tampilan Daftar Data & Chart
    @GetMapping
    public String index(Model model, Principal principal) {
        User user = getAuthenticatedUser(principal);
        List<Product> products = productService.getAllProductsByUser(user);
        model.addAttribute("products", products);

        // Data untuk Chart (Nama Produk vs Stok)
        List<String> names = products.stream().map(Product::getName).collect(Collectors.toList());
        List<Integer> stocks = products.stream().map(Product::getStock).collect(Collectors.toList());
        
        model.addAttribute("chartLabels", names);
        model.addAttribute("chartData", stocks);

        return ConstUtil.TEMPLATE_PAGES_PRODUCTS_INDEX;
    }

    // 2. Fitur Tambah Data (Form)
    @GetMapping("/create")
    public String showCreate(Model model) {
        model.addAttribute("productForm", new ProductForm());
        return ConstUtil.TEMPLATE_PAGES_PRODUCTS_CREATE;
    }

    // Proses Tambah
   @PostMapping("/create")
    public String create(@Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult result,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {
        
        System.out.println("=== MULAI DEBUG CREATE PRODUCT ===");

        // 1. Cek Validasi Form
        if (result.hasErrors()) {
            System.out.println("❌ VALIDASI GAGAL!");
            // Cetak semua error ke konsol biar kelihatan salahnya di mana
            result.getAllErrors().forEach(error -> {
                System.out.println("   - Error: " + error.getDefaultMessage());
            });
            return ConstUtil.TEMPLATE_PAGES_PRODUCTS_CREATE;
        }

        try {
            User user = getAuthenticatedUser(principal);
            System.out.println("✅ User ditemukan: " + (user != null ? user.getName() : "NULL"));

            if (user == null) {
                System.out.println("❌ User NULL, redirect logout");
                return "redirect:/auth/logout";
            }

            // 2. Cek Data yang mau disimpan
            System.out.println("📦 Data Form:");
            System.out.println("   - Nama: " + form.getName());
            System.out.println("   - SKU: " + form.getSku());
            System.out.println("   - Kategori: " + form.getCategory());
            System.out.println("   - Harga: " + form.getPrice());

            // 3. Eksekusi Simpan
            productService.saveProduct(form, user);
            System.out.println("✅ BERHASIL MEMANGGIL SERVICE SAVE");
            
            redirectAttributes.addFlashAttribute("success", "Produk berhasil ditambahkan!");

        } catch (IOException e) {
            System.out.println("❌ ERROR IO: " + e.getMessage());
            result.rejectValue("image", "error.productForm", "Gagal mengupload gambar");
            return ConstUtil.TEMPLATE_PAGES_PRODUCTS_CREATE;
        } catch (Exception e) {
            System.out.println("❌ ERROR LAIN: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== SELESAI DEBUG ===");
        return "redirect:/products";
    }

    // 3. Fitur Tampilan Detail Data
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        Product product = productService.getProductById(id);
        if (product == null) return "redirect:/products";
        model.addAttribute("product", product);
        return ConstUtil.TEMPLATE_PAGES_PRODUCTS_DETAIL;
    }

    // 4. Fitur Ubah Data (Form)
    @GetMapping("/edit/{id}")
    public String showEdit(@PathVariable UUID id, Model model) {
        Product product = productService.getProductById(id);
        if (product == null) return "redirect:/products";

        ProductForm form = new ProductForm();
        form.setName(product.getName());
        form.setPrice(product.getPrice());
        form.setStock(product.getStock());
        form.setDescription(product.getDescription());
        // Image tidak di-set di form karena multipart, biarkan kosong jika tidak diubah

        model.addAttribute("productForm", form);
        model.addAttribute("productId", id);
        return ConstUtil.TEMPLATE_PAGES_PRODUCTS_EDIT;
    }

    // Proses Ubah (Termasuk Ubah Gambar)
    @PostMapping("/edit/{id}")
    public String update(@PathVariable UUID id,
                         @Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("productId", id);
            return ConstUtil.TEMPLATE_PAGES_PRODUCTS_EDIT;
        }

        try {
            productService.updateProduct(id, form);
            redirectAttributes.addFlashAttribute("success", "Produk berhasil diperbarui!");
        } catch (IOException e) {
            model.addAttribute("productId", id);
            result.rejectValue("image", "error.productForm", "Gagal mengupload gambar");
            return ConstUtil.TEMPLATE_PAGES_PRODUCTS_EDIT;
        }

        return "redirect:/products";
    }

    // 5. Fitur Hapus Data
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("success", "Produk berhasil dihapus!");
        return "redirect:/products";
    }
}