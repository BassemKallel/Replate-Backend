package com.replate.replatebackend.controller;

import com.replate.replatebackend.enums.AnnouncementType;
import com.replate.replatebackend.model.Announcement;
import com.replate.replatebackend.payload.AnnouncementRequest;
import com.replate.replatebackend.service.AnnouncementService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/announcements")
@AllArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * RDT-5: Publier une annonce
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MERCHANT')")
    public ResponseEntity<?> createAnnouncement(
            // --- Remplacement du @RequestPart JSON ---
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("type") AnnouncementType type, // Spring convertit la chaîne en Enum
            @RequestParam("quantity") Double quantity,
            @RequestParam("foodType") String foodType,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(value="pickupTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pickupTime,
            @RequestParam("address") String address,
            // --- Fichier ---
            @RequestParam("imageFile") MultipartFile imageFile,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        try {
            Announcement newAnnouncement = announcementService.createAnnouncement(
                    userEmail, title, description, type, quantity, foodType, expiryDate, pickupTime, address, imageFile
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(newAnnouncement);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Échec de l'upload de l'image : " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * RDT-6: Modifier une annonce
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MERCHANT')")
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable Long id,
            @RequestPart(value="announcementData", required = false) AnnouncementRequest request,
            @RequestPart(value="imageFile", required = false) MultipartFile imageFile,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        try {
            Announcement updated = announcementService.updateAnnouncement(id, userEmail, request, imageFile);
            return ResponseEntity.ok(updated);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Échec de l'upload de la nouvelle image : " + e.getMessage());
        }
    }

    /**
     * RDT-7: Supprimer une annonce
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MERCHANT')")
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Lire une annonce par ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Tous les utilisateurs connectés peuvent voir
    public ResponseEntity<Object> getAnnouncementById(@PathVariable Long id) {
        try {
            Announcement announcement = announcementService.getAnnouncementById(id);
            return ResponseEntity.ok(announcement);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Annonce non trouvée avec l'ID: " + id);
        }
    }

}