package com.replate.replatebackend.controller;

import com.replate.replatebackend.enums.ModerationStatus;
import com.replate.replatebackend.model.Announcement;
import com.replate.replatebackend.model.User;
import com.replate.replatebackend.service.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@AllArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')") // Sécurité globale : seuls les ADMINS peuvent utiliser ce contrôleur
public class AdminController {

    private final AdminService adminService;

    /**
     * RDT-4: Liste les utilisateurs en attente de vérification.
     *
     */
    @GetMapping("/users/pending")
    public ResponseEntity<List<User>> getPendingUsers() {
        List<User> unverifiedUsers = adminService.getUnverifiedUsers();
        return ResponseEntity.ok(unverifiedUsers);
    }

    /**
     * RDT-4: Valide le compte d'un utilisateur.
     */
    @PutMapping("/users/validate/{userId}")
    public ResponseEntity<?> validateUserAccount(@PathVariable Long userId) {
        try {
            User validatedUser = adminService.validateAccount(userId);
            return ResponseEntity.ok(validatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * RDT-30: Liste les annonces en attente de modération.
     */
    @GetMapping("/announcements/pending")
    public ResponseEntity<List<Announcement>> getPendingAnnouncements() {
        List<Announcement> pendingAnnouncements = adminService.getPendingAnnouncements();
        return ResponseEntity.ok(pendingAnnouncements);
    }

    /**
     * RDT-30: Modère manuellement une annonce (Approuve/Rejette).
     */
    @PutMapping("/announcements/moderate/{announcementId}")
    public ResponseEntity<?> moderateAnnouncement(
            @PathVariable Long announcementId,
            @RequestParam ModerationStatus status // ex: ?status=APPROVED
    ) {
        try {
            Announcement moderatedAnnouncement = adminService.moderateAnnouncement(announcementId, status);
            return ResponseEntity.ok(moderatedAnnouncement);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}