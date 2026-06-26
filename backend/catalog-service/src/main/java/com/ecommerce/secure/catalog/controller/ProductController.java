package com.ecommerce.secure.catalog.controller;
import com.ecommerce.secure.catalog.entity.Product;
import com.ecommerce.secure.catalog.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@Tag(name = "Products", description = "Quản lý danh mục sản phẩm - Lấy, tạo, cập nhật, xóa sản phẩm")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    @Operation(summary = "Lấy tất cả sản phẩm", description = "Trả về danh sách toàn bộ sản phẩm đang hoạt động")
    @ApiResponse(responseCode = "200", description = "Thành công")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy sản phẩm theo ID", description = "Trả về thông tin chi tiết của 1 sản phẩm")
    @ApiResponse(responseCode = "200", description = "Tìm thấy sản phẩm")
    @ApiResponse(responseCode = "400", description = "Không tìm thấy sản phẩm")
    public Product getProductById(@Parameter(description = "ID của sản phẩm") @PathVariable Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    @PostMapping
    @Operation(summary = "Tạo sản phẩm mới", description = "Yêu cầu quyền Admin", security = @SecurityRequirement(name = "Bearer Auth"))
    @ApiResponse(responseCode = "200", description = "Tạo thành công")
    public Product createProduct(@RequestBody Product product) {
        product.setIsActive(true);
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sản phẩm", description = "Yêu cầu quyền Admin", security = @SecurityRequirement(name = "Bearer Auth"))
    @ApiResponse(responseCode = "200", description = "Cập nhật thành công")
    public Product updateProduct(@Parameter(description = "ID của sản phẩm") @PathVariable Long id, @RequestBody Product productDetails) {
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
    @Operation(summary = "Xóa mềm sản phẩm", description = "Đặt isActive=false, không xóa khỏi DB. Yêu cầu quyền Admin", security = @SecurityRequirement(name = "Bearer Auth"))
    @ApiResponse(responseCode = "200", description = "Xóa thành công")
    public void deleteProduct(@Parameter(description = "ID của sản phẩm") @PathVariable Long id) {
        productRepository.findById(id).ifPresent(product -> {
            product.setIsActive(false);
            productRepository.save(product);
        });
    }
}
