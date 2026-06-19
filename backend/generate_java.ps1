$basePath = "d:\HKII-2026\MatMaUngDung\project\backend\catalog-service\src\main\java\com\ecommerce\secure\catalog"

mkdir -Force "$basePath\entity"
mkdir -Force "$basePath\repository"
mkdir -Force "$basePath\controller"

# Product Entity
$productCode = @"
package com.ecommerce.secure.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = `"products`")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = `"category_id`")
    private Long categoryId;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private Integer stock;
    @Column(name = `"image_url`")
    private String imageUrl;
    @Column(name = `"is_active`")
    private Boolean isActive;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
"@
Set-Content -Path "$basePath\entity\Product.java" -Value $productCode

# Product Repository
$productRepoCode = @"
package com.ecommerce.secure.catalog.repository;
import com.ecommerce.secure.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
"@
Set-Content -Path "$basePath\repository\ProductRepository.java" -Value $productRepoCode

# Product Controller
$productCtrlCode = @"
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
}
"@
Set-Content -Path "$basePath\controller\ProductController.java" -Value $productCtrlCode

# User Entity
$userCode = @"
package com.ecommerce.secure.catalog.entity;

import jakarta.persistence.*;

@Entity
@Table(name = `"users`")
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private String role;
    @Column(name = `"is_active`")
    private Boolean isActive;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
"@
Set-Content -Path "$basePath\entity\User.java" -Value $userCode

# User Repository
$userRepoCode = @"
package com.ecommerce.secure.catalog.repository;
import com.ecommerce.secure.catalog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
}
"@
Set-Content -Path "$basePath\repository\UserRepository.java" -Value $userRepoCode

# User Controller
$userCtrlCode = @"
package com.ecommerce.secure.catalog.controller;
import com.ecommerce.secure.catalog.entity.User;
import com.ecommerce.secure.catalog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public void softDeleteUser(@PathVariable String id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setIsActive(false);
            userRepository.save(user);
        });
    }
}
"@
Set-Content -Path "$basePath\controller\UserController.java" -Value $userCtrlCode

echo "Java files generated successfully."
