package com.example.dynamicform.auth.application;

import com.example.dynamicform.user.domain.UserRole;
import com.example.dynamicform.user.infrastructure.persistence.UserEntity;
import com.example.dynamicform.user.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String displayName;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap-admin.email}") String email,
                          @Value("${app.bootstrap-admin.display-name}") String displayName,
                          @Value("${app.bootstrap-admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.displayName = displayName;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        UserEntity admin = UserEntity.create(
                normalizedEmail,
                displayName.trim(),
                passwordEncoder.encode(password),
                UserRole.ADMIN
        );

        userRepository.save(admin);
        log.info("Created local bootstrap admin user with email {}", normalizedEmail);
    }
}
