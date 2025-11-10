package com.replate.replatebackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entité pour un utilisateur de type "Merchant".
 * Hérite de User.
 */
@Entity
@DiscriminatorValue("MERCHANT") // Valeur dans la colonne "role"
@Data // Utilise Lombok
@EqualsAndHashCode(callSuper = true) // Important pour l'héritage avec @Data
@NoArgsConstructor // Constructeur vide requis par JPA
public class Merchant extends User {

    // Champ spécifique de votre diagramme UML
    @Column(name = "donation_count")
    private int donationCount = 0;

    // --- Champ ajouté pour le stockage de fichiers ---

    @Column(name = "verification_document_url")
    private String verificationDocumentUrl; // Nom du fichier de vérification

    /**
     * Constructeur principal pour la logique d'inscription (appelé par AuthController).
     */
    public Merchant(String name, String email, String password, String phone, String address) {
        // Appelle le constructeur de la classe parente "User"
        super(name, email, password, phone, address);
        this.donationCount = 0; // Initialise la valeur spécifique
    }
}