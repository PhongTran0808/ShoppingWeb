package com.ecommerce.secure.catalog.controller;
import com.ecommerce.secure.catalog.entity.Product;
import com.ecommerce.secure.catalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        product.setIsActive(true);
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setName(productDetails.getName());
        product.setSlug(productDetails.getSlug());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStock(productDetails.getStock());
        product.setImageUrl(productDetails.getImageUrl());
        product.setCategoryId(productDetails.getCategoryId());
        if (productDetails.getIsActive() != null) {
            product.setIsActive(productDetails.getIsActive());
        }
        return productRepository.save(product);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productRepository.findById(id).ifPresent(product -> {
            product.setIsActive(false);
            productRepository.save(product);
        });
    }
}
