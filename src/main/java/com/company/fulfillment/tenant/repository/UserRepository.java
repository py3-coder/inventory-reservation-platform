package com.company.fulfillment.tenant.repository;

import com.company.fulfillment.tenant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmail(
            UUID tenantId,
            String email
    );
}