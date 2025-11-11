package com.replate.replatebackend.service;

import com.replate.replatebackend.enums.ModerationStatus;
import com.replate.replatebackend.enums.ERole;
import com.replate.replatebackend.model.Announcement;
import com.replate.replatebackend.model.User;
import com.replate.replatebackend.repository.AnnouncementRepository;
import com.replate.replatebackend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor // Injecte les repositories
public class AdminService {

    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;

    /**
     * Logique pour RDT-4 : Récupère les utilisateurs non-admins qui ne sont pas vérifiés.
     *
     */
    public List<User> getUnverifiedUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isVerified()) // Le champ 'verified' de l'UML
                .filter(user -> user.getRole() != ERole.ADMIN) // Exclut les admins eux-mêmes
                .collect(Collectors.toList());
    }

    /**
     * Logique pour RDT-4 : Valide le compte d'un utilisateur.
     * Implémente + verifyUser(user_id).
     */
    public User validateAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        if (user.isVerified()) {
            throw new RuntimeException("Le compte utilisateur est déjà vérifié.");
        }

        user.setVerified(true); // Change le statut
        return userRepository.save(user);
    }

    /**
     * Récupère la liste des annonces en attente de modération manuelle.
     */
    public List<Announcement> getPendingAnnouncements() {
        return announcementRepository.findByModerationStatus(ModerationStatus.PENDING_REVIEW);
    }

    /**
     * Modère manuellement une annonce (Approuve ou Rejette).
     *
     */
    public Announcement moderateAnnouncement(Long announcementId, ModerationStatus newStatus) {
        // Un admin ne peut pas remettre une annonce en "pending" manuellement
        if (newStatus == ModerationStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Le statut ne peut pas être remis à PENDING_REVIEW.");
        }

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée avec l'ID: " + announcementId));

        announcement.setModerationStatus(newStatus);

        // On réinitialise le score après une revue humaine
        if (newStatus == ModerationStatus.APPROVED) {
            announcement.setModerationScore(0.0);
        }

        return announcementRepository.save(announcement);
    }
}