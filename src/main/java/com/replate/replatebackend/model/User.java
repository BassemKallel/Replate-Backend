package com.replate.replatebackend.model;

import com.replate.replatebackend.enums.ERole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data; // Inclut @Getter, @Setter, @ToString, @EqualsAndHashCode
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Classe de base abstraite pour tous les utilisateurs de l'application.
 * Utilise une stratégie d'héritage SINGLE_TABLE avec une colonne "role".
 */
@Entity
@Table(name = "users", // Nom de la table unique en base de données
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email") // L'email doit être unique
        })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // Stratégie d'héritage
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING) // Colonne de type
@Data // Utilise Lombok pour les getters, setters, etc.
@NoArgsConstructor // Constructeur vide requis par JPA
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Champs de votre diagramme UML
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    @Column(name = "is_verified")
    private boolean isVerified = false; // "verified" sur l'UML

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Champ pour le mapping de l'héritage (colonne "role")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", insertable = false, updatable = false)
    private ERole role;

    // --- Champs ajoutés pour le stockage de fichiers ---

    @Column(name = "profile_image_url")
    private String profileImageUrl; // Nom du fichier de l'image de profil

    /**
     * Constructeur principal pour la logique d'inscription.
     * Appelé par les classes enfants (Admin, Merchant, Association).
     */
    public User(String name, String email, String password, String phone, String address) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
    }
}