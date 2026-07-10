package com.kirana.controller;

import com.kirana.model.*;
import com.kirana.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @GetMapping
    public ResponseEntity<List<Product>> getAllActiveProducts() {
        return ResponseEntity.ok(productRepository.findByIsActiveTrue());
    }

    @GetMapping("/admin-all")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Product>> getAllProductsForAdmin() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String query) {
        return ResponseEntity.ok(productRepository.searchProducts(query));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            Integer categoryId = (Integer) body.get("categoryId");
            String description = (String) body.get("description");
            double price = Double.parseDouble(body.get("price").toString());
            String unit = (String) body.get("unit");
            int stockQuantity = Integer.parseInt(body.get("stockQuantity").toString());

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            Product product = Product.builder()
                    .name(name)
                    .category(category)
                    .description(description)
                    .price(java.math.BigDecimal.valueOf(price))
                    .unit(unit)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            Product savedProduct = productRepository.save(product);

            Inventory inventory = Inventory.builder()
                    .product(savedProduct)
                    .stockQuantity(stockQuantity)
                    .lowStockThreshold(5)
                    .updatedAt(LocalDateTime.now())
                    .build();

            inventoryRepository.save(inventory);

            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> toggleProductActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        product.setIsActive(body.get("isActive"));
        productRepository.save(product);
        return ResponseEntity.ok(Map.of("message", "Product active status updated successfully"));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Inventory>> getLowStockItems() {
        return ResponseEntity.ok(inventoryRepository.findLowStockAlerts());
    }
}
