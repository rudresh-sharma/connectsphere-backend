package com.connectsphere.auth.config;

import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.entity.Role;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configures Admin Account Initializer infrastructure for the service.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAccountProperties adminAccountProperties;

    public AdminAccountInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AdminAccountProperties adminAccountProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminAccountProperties = adminAccountProperties;
    }
/**
 * Performs the run operation.
 * @param args application startup arguments
 */

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        String adminUsername = normalizeUsername(adminAccountProperties.getUsername());
        String adminEmail = normalizeEmail(adminAccountProperties.getEmail());

        User systemAdmin = userRepository.findByUsername(adminUsername)
                .or(() -> userRepository.findByEmail(adminEmail))
                .orElseGet(User::new);

        systemAdmin.setUsername(adminUsername);
        systemAdmin.setEmail(adminEmail);
        systemAdmin.setFullName(adminAccountProperties.getFullName().trim());
        systemAdmin.setPasswordHash(passwordEncoder.encode(adminAccountProperties.getPassword()));
        systemAdmin.setRole(Role.ADMIN);
        systemAdmin.setProvider(Provider.LOCAL);
        systemAdmin.setProviderId(null);
        systemAdmin.setActive(true);
        userRepository.save(systemAdmin);

        List<User> otherAdmins = userRepository.findAllByRole(Role.ADMIN).stream()
                .filter(user -> !adminUsername.equals(user.getUsername()))
                .toList();

        for (User otherAdmin : otherAdmins) {
            otherAdmin.setRole(Role.USER);
        }
        if (!otherAdmins.isEmpty()) {
            userRepository.saveAll(otherAdmins);
            log.warn("Demoted {} extra admin account(s). System admin username={}", otherAdmins.size(), adminUsername);
        }
    }

    private String normalizeUsername(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
