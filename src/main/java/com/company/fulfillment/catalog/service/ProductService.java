package com.company.fulfillment.catalog.service;

import com.company.fulfillment.catalog.dto.ProductRequest;
import com.company.fulfillment.catalog.dto.ProductResponse;
import com.company.fulfillment.catalog.entity.Product;
import com.company.fulfillment.catalog.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    @Transactional
    public ProductResponse createProduct(UUID tenantId, ProductRequest request) {
        if (productRepository
                .findByTenantIdAndSku(
                        tenantId,
                        request.sku()
                )
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Product with SKU already exists"
            );
        }

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());

        try {
            product = productRepository.save(product);

        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "Product with SKU already exists",
                    exception
            );
        }

        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID tenantId, UUID productId) {

        Product product = productRepository
                        .findByIdAndTenantId(productId, tenantId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Product not found"
                                )
                        );

        return toResponse(product);
    }
    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTenantId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

