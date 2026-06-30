package com.aes.exam.auth;

import com.aes.exam.auth.repository.UserRepository;
import com.aes.exam.auth.service.PasswordService;
import com.aes.exam.common.config.AesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class AdminBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);

    private final AesProperties properties;
    private final PasswordService passwordService;
    private final UserRepository userRepository;

    public AdminBootstrapper(AesProperties properties, PasswordService passwordService, UserRepository userRepository) {
        this.properties = properties;
        this.passwordService = passwordService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AesProperties.InitialAdminProperties admin = properties.getAuth().getInitialAdmin();
        userRepository.findByUsername(admin.getUsername()).ifPresentOrElse(
            ignored -> log.info("Initial administrator already exists: {}", admin.getUsername()),
            () -> {
                userRepository.createUser(
                    admin.getUsername(),
                    passwordService.hash(admin.getPassword()),
                    UserRole.ADMIN,
                    admin.getRealName()
                );
                log.info("Initial administrator created: {}", admin.getUsername());
            }
        );
    }
}
