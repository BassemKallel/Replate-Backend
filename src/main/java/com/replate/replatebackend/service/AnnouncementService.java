package com.replate.replatebackend.service;

import com.replate.replatebackend.enums.AnnouncementType;
import com.replate.replatebackend.enums.ModerationStatus;
import com.replate.replatebackend.model.Announcement;
import com.replate.replatebackend.model.User;
import com.replate.replatebackend.model.Merchant;
import com.replate.replatebackend.payload.AnnouncementRequest;
import com.replate.replatebackend.repository.AnnouncementRepository;
import com.replate.replatebackend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // (Pas de services IA, modération manuelle)

    public Announcement createAnnouncement(
            String userEmail,
            // --- Paramètres individuels au lieu d'un DTO ---
            String title,
            String description,
            AnnouncementType type,
            Double quantity,
            String foodType,
            LocalDate expiryDate,
            LocalDateTime pickupTime,
            String address,
            // --- Fichier ---
            MultipartFile imageFile
    ) throws IOException {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        if (!(user instanceof Merchant)) {
            throw new AccessDeniedException("Seuls les commerçants (Merchants) peuvent créer des annonces.");
        }
        Merchant merchant = (Merchant) user;

        String imageUrl = fileStorageService.saveFile(imageFile, "announcements");

        Announcement announcement = new Announcement();

        // --- Assignation des champs individuels ---
        announcement.setTitle(title);
        announcement.setDescription(description);
        announcement.setType(type);
        announcement.setQuantity(quantity);
        announcement.setFoodType(foodType);
        announcement.setExpiryDate(expiryDate);
        announcement.setPickupTime(pickupTime);
        announcement.setAddress(address);

        announcement.setImageUrl(imageUrl);
        announcement.setDonor(merchant);

        // Modération manuelle par défaut
        announcement.setModerationStatus(ModerationStatus.PENDING_REVIEW);
        announcement.setModerationScore(null);

        return announcementRepository.save(announcement);
    }
    public Announcement updateAnnouncement(
            Long announcementId,
            String userEmail,
            AnnouncementRequest request,
            MultipartFile imageFile
    ) throws IOException {

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        if (!announcement.getDonor().getId().equals(user.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier cette annonce.");
        }

        if (request != null) {
            announcement.setTitle(request.getTitle());
            announcement.setDescription(request.getDescription());
            // ... (etc.)
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            fileStorageService.deleteFile(announcement.getImageUrl(), "announcements"); // Supprime l'ancienne
            String newImageUrl = fileStorageService.saveFile(imageFile, "announcements"); // Ajoute la nouvelle
            announcement.setImageUrl(newImageUrl);
        }

        // Repasse en modération
        announcement.setModerationStatus(ModerationStatus.PENDING_REVIEW);
        return announcementRepository.save(announcement);
    }

    public void deleteAnnouncement(Long announcementId, String userEmail) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        if (!announcement.getDonor().getId().equals(user.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer cette annonce.");
        }

        // Nettoyage des fichiers
        fileStorageService.deleteFile(announcement.getImageUrl(), "announcements");
        announcementRepository.delete(announcement);
    }

    public Announcement getAnnouncementById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée avec l'ID: " + id));
    }

    public List<Announcement> getAllUnverifiedAnnouncements() {
        return announcementRepository.findByModerationStatus(ModerationStatus.PENDING_REVIEW);
    }
}