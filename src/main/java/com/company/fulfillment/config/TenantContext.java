package com.company.fulfillment.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.TimeZone;
import java.util.UUID;

public final class TenantContext {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    private TenantContext() {}

    public static JwtAuthentication getAuthentication() {

        Object authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (!(authentication instanceof JwtAuthentication jwt)) {
            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        return jwt;
    }

    public static UUID getUserId() {
        return getAuthentication().getUserId();
    }

    public static UUID getTenantId() {
        return getAuthentication().getTenantId();
    }

    public static void validateTenant(UUID requestTenantId) {

        UUID jwtTenantId = TenantContext.getTenantId();

        if (!jwtTenantId.equals(requestTenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant mismatch"
            );
        }
    }
}