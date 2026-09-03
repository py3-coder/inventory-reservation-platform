package com.company.fulfillment.catalog.repository;

import com.company.fulfillment.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByTenantIdAndSku(
            UUID tenantId,
            String sku
    );
}