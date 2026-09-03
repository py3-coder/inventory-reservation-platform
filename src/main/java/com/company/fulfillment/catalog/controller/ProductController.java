package com.company.fulfillment.catalog.controller;

import com.company.fulfillment.catalog.dto.ProductRequest;
import com.company.fulfillment.catalog.dto.ProductResponse;
import com.company.fulfillment.catalog.service.ProductService;
import com.company.fulfillment.config.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody ProductRequest request) {

        TenantContext.validateTenant(tenantId);
        ProductResponse response =
                productService.createProduct(
                        tenantId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID productId) {

        TenantContext.validateTenant(tenantId);
        ProductResponse response =
                productService.getProduct(
                        tenantId,
                        productId
                );
        return ResponseEntity.ok(response);
    }
}
