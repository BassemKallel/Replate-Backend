package com.replate.replatebackend.controller;

import com.replate.replatebackend.enums.ERole;
import com.replate.replatebackend.model.Admin;
import com.replate.replatebackend.model.Merchant;
import com.replate.replatebackend.model.User;
import com.replate.replatebackend.payload.JwtResponse;
import com.replate.replatebackend.payload.LoginRequest;
import com.replate.replatebackend.repository.UserRepository;
import com.replate.replatebackend.security.JwtUtils;
import com.replate.replatebackend.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@AllArgsConstructor // Injecte tous les services via le constructeur
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Les services requis, injectés par Lombok @AllArgsConstructor
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final FileStorageService fileStorageService; // Le service de stockage

    /**
     * Endpoint de connexion (RDT-71).
     * Accepte du JSON.
     */
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        // Authentifie en utilisant l'email (notre UserDetailsServiceImpl s'attend à un email)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        // Récupère le rôle unique
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Erreur: Rôle non trouvé."));

        // Récupère l'utilisateur depuis la BDD pour son ID
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return ResponseEntity.ok(new JwtResponse(jwt,
                user.getId(),
                user.getEmail(),
                role));
    }

    /**
     * Endpoint d'inscription (RDT-3).
     * Accepte du multipart/form-data (texte + fichiers).
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(
            // --- Champs de texte ---
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("phone") String phone,
            @RequestParam("address") String address,
            @RequestParam("role") String strRole,

            // --- Fichiers ---
            @RequestParam("profileImage") MultipartFile profileImageFile,
            @RequestParam(value="verificationDocument", required = false) MultipartFile verificationDocumentFile
    ) {

        // 1. Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Erreur: Email déjà utilisé!");
        }

        // 2. Créer l'objet User (en mémoire)
        User user;
        ERole role;
        String hashedPassword = encoder.encode(password);

        try {
            role = ERole.valueOf(strRole.toUpperCase());

            if (role == ERole.ADMIN) {
                user = new Admin(name, email, hashedPassword, phone, address);
            } else if (role == ERole.MERCHANT) {
                user = new Merchant(name, email, hashedPassword, phone, address);
            } else {
                return ResponseEntity.badRequest().body("Erreur: Rôle non supporté!");
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Erreur: Rôle invalide!");
        }

        try {
            userRepository.save(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création initiale de l'utilisateur.");
        }

        // 4. Gérer les fichiers (Maintenant que nous avons un ID)
        String subFolderUsers = "users";
        String subFolderVerification = "verification";

        try {
            // Sauvegarder l'image de profil
            String profileImageName = fileStorageService.saveFile(profileImageFile, subFolderUsers);
            user.setProfileImageUrl(profileImageName); // On sauvegarde le nom unique

            // Sauvegarder le document de vérification (si Merchant ou Association)
            if (role == ERole.MERCHANT) {
                if (verificationDocumentFile != null && !verificationDocumentFile.isEmpty()) {

                    String docName = fileStorageService.saveFile(verificationDocumentFile, subFolderVerification);

                    if (role == ERole.MERCHANT) {
                        ((Merchant) user).setVerificationDocumentUrl(docName);
                    }
                } else {
                    ResponseEntity.badRequest().body("Erreur: Document de vérification requis.");
                    // Optionnel: vous pouvez rejeter l'inscription si le doc est manquant
                    // return ResponseEntity.badRequest().body("Erreur: Document de vérification requis.");
                }
            }

        } catch (IOException e) {
            // En cas d'échec de l'upload, renvoyer une erreur
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la sauvegarde des fichiers: " + e.getMessage());
        }

        // 5. Sauvegarde N°2 (Mettre à jour l'utilisateur avec les noms de fichiers)
        userRepository.save(user);

        // 6. Renvoyer la réponse personnalisée
        if (role == ERole.MERCHANT) {
            String message = "Inscription réussie. Votre compte est en attente de vérification par un administrateur qui validera vos documents.";
            return ResponseEntity.ok(message);
        }

        return ResponseEntity.ok("Utilisateur Admin enregistré avec succès!");
    }
}