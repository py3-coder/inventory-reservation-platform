package com.company.fulfillment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().equals("/api/v1/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            System.out.println("NO BEARER TOKEN");
            filterChain.doFilter(request, response);
            return;
        }
        String token = authorization.substring(7);

        try {
            UUID userId = jwtService.extractUserId(token);
            UUID tenantId = jwtService.extractTenantId(token);
            String role = jwtService.extractRole(token);


            JwtAuthentication authentication = new JwtAuthentication(userId, tenantId, role);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {

            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}