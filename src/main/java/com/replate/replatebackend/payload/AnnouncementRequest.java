package com.replate.replatebackend.payload;

import com.replate.replatebackend.enums.AnnouncementType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) pour transporter les données d'un formulaire
 * de création ou de mise à jour d'annonce.
 * Les champs sont basés sur l'entité Announcement.
 */
@Data // Génère les getters, setters, etc.
public class AnnouncementRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "Le type (SALE ou DONATION) est obligatoire")
    private AnnouncementType type;

    @NotNull(message = "La quantité est obligatoire")
    private Double quantity;

    @NotBlank(message = "Le type de nourriture est obligatoire")
    private String foodType;

    @NotNull(message = "La date d'expiration est obligatoire")
    @Future(message = "La date d'expiration doit être dans le futur")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Accepte "2025-12-31"
    private LocalDate expiryDate;

    // Ce champ est optionnel
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) // Accepte "2025-11-10T14:30:00"
    private LocalDateTime pickupTime;

    @NotBlank(message = "L'adresse de récupération est obligatoire")
    private String address;
}