package org.delcom.app.views;

import org.delcom.app.entities.Product;
import org.delcom.app.entities.User;
import org.delcom.app.services.ProductService;
import org.delcom.app.services.UserService;
import org.delcom.app.utils.ConstUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
public class HomeView {

    private final UserService userService;
    private final ProductService productService;

    public HomeView(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        // 1. Ambil data User yang sedang login
        User user = null;
        if (principal != null) {
            user = userService.getUserByEmail(principal.getName());
        }
        
        // Kirim object user ke template (agar ${user.name} berfungsi)
        model.addAttribute("user", user);

        if (user != null) {
            // 2. Ambil semua produk milik user tersebut
            List<Product> products = productService.getAllProductsByUser(user);

            // 3. Hitung Statistik
            
            // a. Total Jenis Barang (Jumlah baris data)
            int totalProducts = products.size();

            // b. Total Stok Fisik (Penjumlahan stok semua barang)
            int totalStock = products.stream()
                    .mapToInt(Product::getStock)
                    .sum();

            // c. Estimasi Nilai Aset (Harga * Stok untuk setiap barang, lalu dijumlahkan)
            BigDecimal totalAssetValue = products.stream()
                    .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 4. Masukkan ke Model
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalStock", totalStock);
            model.addAttribute("totalAssetValue", totalAssetValue);
        } else {
            // Default value jika user null (untuk jaga-jaga)
            model.addAttribute("totalProducts", 0);
            model.addAttribute("totalStock", 0);
            model.addAttribute("totalAssetValue", BigDecimal.ZERO);
        }

        return ConstUtil.TEMPLATE_PAGES_HOME;
    }
}