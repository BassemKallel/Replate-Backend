package com.replate.replatebackend.model;

import com.replate.replatebackend.enums.AnnouncementType;
import com.replate.replatebackend.enums.ModerationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant une annonce, basée sur le diagramme de classes.
 */
@Entity
@Table(name = "announcements")
@Data // Inclut @Getter, @Setter, @ToString, @EqualsAndHashCode
@NoArgsConstructor // Requis par JPA
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // "annonce_id"

    @NotBlank
    private String title;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String description; // "description"

    @NotNull
    @Enumerated(EnumType.STRING)
    private AnnouncementType type; // "type"

    @NotNull
    private Double quantity; // "quantity"

    @NotBlank
    private String foodType; // "food_type"

    @NotNull
    @Future(message = "La date d'expiration doit être dans le futur")
    private LocalDate expiryDate; // "expiry_date"

    private LocalDateTime pickupTime; // "pickup_time"

    @NotBlank
    private String address;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING_REVIEW; // "moderation_status"

    @Column(name = "moderation_score")
    private Double moderationScore; // Pour l'IA

    @Column(name = "created_at", updatable = false)
    private LocalDateTime creationDate = LocalDateTime.now(); // "creation_date"

    @Column(name = "image_url")
    private String imageUrl; // Nom du fichier image

    // --- Relation ---

    /**
     * L'utilisateur (Merchant ou Association) qui a posté cette annonce.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY) // LAZY = Ne charge pas l'utilisateur sauf si on le demande
    @JoinColumn(name = "donor_id", nullable = false) // Clé étrangère
    private Merchant donor; // "donor_id"
}