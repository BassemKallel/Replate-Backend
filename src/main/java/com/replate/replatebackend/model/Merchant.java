package com.replate.replatebackend.model;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor; // <-- AJOUTEZ CECI
import lombok.Setter;

@Entity
@DiscriminatorValue("MERCHANT")
@Getter
@Setter
@NoArgsConstructor // JPA a besoin d'un constructeur vide
public class Merchant extends User {

    private int donation_count;

    // V-- AJOUTEZ MANUELLEMENT CE CONSTRUCTEUR --V
    public Merchant(String name, String email, String password, String phone, String address) {
        // Il appelle le constructeur parent (User)
        super(name, email, password, phone, address);
        this.donation_count = 0; // Initialise son propre champ
    }
}