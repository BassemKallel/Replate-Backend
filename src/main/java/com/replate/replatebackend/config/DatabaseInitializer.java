package com.replate.replatebackend.config;

import com.replate.replatebackend.model.Admin;
import com.replate.replatebackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@replate.com";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            System.out.println(">>> Création du compte Admin par défaut...");

            Admin adminUser = new Admin(
                    "Admin Principal",
                    adminEmail,
                    passwordEncoder.encode("ReplateAdminPass123"), // Mot de passe haché !
                    "00112233",
                    "Siège social Replate"
            );

            adminUser.setVerified(true);

            userRepository.save(adminUser);

            System.out.println(">>> Compte Admin créé avec succès !");
        } else {
            System.out.println(">>> Compte Admin existe déjà. Aucune action requise.");
        }
    }
}