package com.company.fulfillment.tenant.service;

import com.company.fulfillment.config.JwtService;
import com.company.fulfillment.tenant.dto.LoginRequest;
import com.company.fulfillment.tenant.dto.LoginResponse;
import com.company.fulfillment.tenant.entity.User;
import com.company.fulfillment.tenant.repository.UserRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByTenantIdAndEmail(
                        request.tenantId(),
                        request.email()
                )
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid credentials"
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Invalid credentials"
            );
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getTenantId(),
                user.getRole()
        );

        return new LoginResponse(
                token,
                "Bearer"
        );
    }
}