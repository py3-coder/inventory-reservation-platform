package com.company.fulfillment.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.UUID;

public class JwtAuthentication extends AbstractAuthenticationToken {

    private final UUID userId;
    private final UUID tenantId;

    public JwtAuthentication(
            UUID userId,
            UUID tenantId,
            String role
    ) {
        super(
                java.util.List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                )
        );

        this.userId = userId;
        this.tenantId = tenantId;

        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}