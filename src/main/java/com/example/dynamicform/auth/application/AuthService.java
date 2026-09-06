package com.example.dynamicform.auth.application;

import com.example.dynamicform.auth.api.dto.AuthResponse;
import com.example.dynamicform.auth.api.dto.LoginRequest;
import com.example.dynamicform.auth.api.dto.RegisterRequest;
import com.example.dynamicform.auth.api.dto.UserResponse;
import com.example.dynamicform.auth.domain.EmailAlreadyExistsException;
import com.example.dynamicform.auth.domain.InvalidCredentialsException;
import com.example.dynamicform.auth.domain.UserAccessDeniedException;
import com.example.dynamicform.user.domain.UserRole;
import com.example.dynamicform.user.domain.UserStatus;
import com.example.dynamicform.user.infrastructure.persistence.UserEntity;
import com.example.dynamicform.user.infrastructure.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        UserEntity user = UserEntity.create(
                email,
                request.displayName().trim(),
                passwordEncoder.encode(request.password()),
                UserRole.USER
        );

        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserAccessDeniedException("User account is disabled");
        }

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserAccessDeniedException("Authenticated user no longer exists"));
        return toUserResponse(user);
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        return new AuthResponse(
                jwtTokenService.createAccessToken(user),
                "Bearer",
                jwtTokenService.accessTokenTtlSeconds(),
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
