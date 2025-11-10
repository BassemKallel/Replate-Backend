package com.replate.replatebackend.model;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor; // <-- AJOUTEZ CECI

@Entity
@DiscriminatorValue("ADMIN")
// @RequiredArgsConstructor // <-- SUPPRIMEZ CECI
@NoArgsConstructor // JPA a besoin d'un constructeur vide
public class Admin extends User {

    // V-- AJOUTEZ MANUELLEMENT CE CONSTRUCTEUR --V
    // C'est celui que l'AuthController appelle
    public Admin(String name, String email, String password, String phone, String address) {
        // Il appelle le constructeur parent (User)
        super(name, email, password, phone, address);
    }
}