package com.replate.replatebackend.controller;

import com.replate.replatebackend.model.Announcement;
import com.replate.replatebackend.payload.AnnouncementRequest;
import com.replate.replatebackend.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Contrôleur API pour la gestion des Annonces (CRUD).
 * Implémente RDT-5, RDT-6, RDT-7.
 */
@RestController
@RequestMapping("/api/announcements") // Endpoint de base
@AllArgsConstructor // Injecte le service via le constructeur
public class AnnouncementController {

    // Le service qui contient toute la logique métier
    private final AnnouncementService announcementService;

    /**
     * Endpoint pour RDT-5: Publier une annonce.
     * Accepte du multipart/form-data (JSON + Fichier).
     */
    @PostMapping

    public ResponseEntity<?> createAnnouncement(
            // @RequestPart pour le JSON (notre DTO)
            @Valid @RequestPart("announcementData") AnnouncementRequest request,
            // @RequestPart pour le fichier image
            @RequestPart("imageFile") MultipartFile imageFile,
            Authentication authentication // Spring injecte l'utilisateur authentifié
    ) {
        // 'authentication.getName()' contient l'email de l'utilisateur connecté
        String userEmail = authentication.getName();
        try {
            Announcement newAnnouncement = announcementService.createAnnouncement(userEmail, request, imageFile);

            // On renvoie 201 Created, comme sur le diagramme de séquence
            return ResponseEntity.status(HttpStatus.CREATED).body(newAnnouncement);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Échec de l'upload de l'image : " + e.getMessage());
        } catch (RuntimeException e) {
            // Intercepte les erreurs comme "Utilisateur non trouvé"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Endpoint pour RDT-6: Modifier une annonce.
     */
    @PutMapping("/{id}") // ex: PUT /api/announcements/123
    @PreAuthorize("hasRole('MERCHANT')") // Sécurisé
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable Long id, // L'ID de l'annonce depuis l'URL
            @RequestPart(value="announcementData", required = false) AnnouncementRequest request,
            @RequestPart(value="imageFile", required = false) MultipartFile imageFile,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        try {
            Announcement updated = announcementService.updateAnnouncement(id, userEmail, request, imageFile);
            return ResponseEntity.ok(updated);

        } catch (AccessDeniedException e) {
            // Si le service dit que l'utilisateur n'est pas le propriétaire
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            // Si l'annonce n'est pas trouvée
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Échec de l'upload de la nouvelle image : " + e.getMessage());
        }
    }

    /**
     * Endpoint pour RDT-7: Supprimer une annonce.
     */
    @DeleteMapping("/{id}") // ex: DELETE /api/announcements/123
    @PreAuthorize("hasRole('MERCHANT')") // Sécurisé
    public ResponseEntity<?> deleteAnnouncement(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        try {
            announcementService.deleteAnnouncement(id, userEmail);
            return ResponseEntity.ok("Annonce supprimée avec succès.");
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            // Si l'annonce n'est pas trouvée
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}