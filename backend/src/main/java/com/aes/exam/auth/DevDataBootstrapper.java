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
@Order(20)
public class DevDataBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataBootstrapper.class);

    private final AesProperties properties;
    private final PasswordService passwordService;
    private final UserRepository userRepository;

    public DevDataBootstrapper(AesProperties properties, PasswordService passwordService, UserRepository userRepository) {
        this.properties = properties;
        this.passwordService = passwordService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.getAuth().getDevSeed().isEnabled()) {
            return;
        }

        seedUser("teacher", "Teacher@123456", UserRole.TEACHER, "测试教师");
        seedUser("student", "Student@123456", UserRole.STUDENT, "测试学生");
    }

    private void seedUser(String username, String password, UserRole role, String realName) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.info("Development user already exists: {}", username);
            return;
        }

        userRepository.createUser(username, passwordService.hash(password), role, realName);
        log.info("Development user created: {}", username);
    }
}
