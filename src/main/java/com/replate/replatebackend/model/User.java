package com.replate.replatebackend.model;


import com.replate.replatebackend.enums.ERole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Users",uniqueConstraints = {
        @UniqueConstraint(
                columnNames = "email")
}
)
@Data
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
// @AllArgsConstructor // <-- SUPPRIMEZ CETTE LIGNE, C'EST LA CAUSE DE L'ERREUR
@NoArgsConstructor // Gardez ceci pour JPA
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    private String phone; // Corrigé (était 'Phone' avec une majuscule)
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", insertable = false , updatable = false)
    private ERole role;
    private boolean isVerified = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    // V-- AJOUTEZ MANUELLEMENT CE CONSTRUCTEUR --V
    // C'est celui que les classes enfants vont appeler
    public User(String name, String email, String password, String phone, String address) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
    }
}