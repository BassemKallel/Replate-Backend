package com.replate.replatebackend.service;

import com.replate.replatebackend.enums.ModerationStatus;
import com.replate.replatebackend.model.Announcement;
import com.replate.replatebackend.model.Merchant;
import com.replate.replatebackend.model.User;
import com.replate.replatebackend.payload.AnnouncementRequest;
import com.replate.replatebackend.repository.AnnouncementRepository;
import com.replate.replatebackend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
@AllArgsConstructor // Injecte tous les services via le constructeur
public class AnnouncementService {

    // Les 4 services requis
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final VisionAnalysisService visionAnalysisService;

    /**
     * Crée une annonce (RDT-5)
     * Étape 1 (Synchrone) : Sauvegarde PENDING_REVIEW
     */
    public Announcement createAnnouncement(
            String userEmail,
            AnnouncementRequest request,
            MultipartFile imageFile
    ) throws IOException {



        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        if (!(user instanceof Merchant)){
            throw new AccessDeniedException("Seuls les marchands peuvent créer des annonces.");
        }

        // 1. Sauvegarder l'image sur le disque
        String imageUrl = fileStorageService.saveFile(imageFile, "announcements");

        // 2. Créer l'objet Annonce
        Announcement announcement = new Announcement();

        // Copie les données du DTO (payload) vers l'Entité
        announcement.setTitle(request.getTitle());
        announcement.setDescription(request.getDescription());
        announcement.setType(request.getType());
        announcement.setQuantity(request.getQuantity());
        announcement.setFoodType(request.getFoodType());
        announcement.setExpiryDate(request.getExpiryDate());
        announcement.setPickupTime(request.getPickupTime());
        announcement.setAddress(request.getAddress());

        // 3. Lier l'annonce à l'image et à l'utilisateur
        announcement.setImageUrl(imageUrl);
        announcement.setDonor((Merchant) user);

        // 4. Mettre le statut par défaut
        announcement.setModerationStatus(ModerationStatus.PENDING_REVIEW);

        // 5. Sauvegarde initiale
        Announcement savedAnnouncement = announcementRepository.save(announcement);

        // 6. Déclencher l'analyse IA en arrière-plan
        processAnnouncementModeration(savedAnnouncement, imageFile);

        // 7. Renvoyer l'annonce (avec statut PENDING_REVIEW) au client
        return savedAnnouncement;
    }

    /**
     * Étape 2 (Asynchrone) : Analyse IA et mise à jour du statut
     */
    @Async // Dit à Spring d'exécuter ceci dans un thread séparé
    public void processAnnouncementModeration(Announcement announcement, MultipartFile imageFile) {

        System.out.println("Début de l'analyse asynchrone pour l'annonce ID: " + announcement.getId());

        try {
            // 1. Appel à l'API Google Vision (étape lente)
            double score = visionAnalysisService.analyzeImageForSafety(imageFile);

            // 2. Définir un seuil de tolérance (ajustez si nécessaire)
            final double MAX_SAFE_SCORE = 5.0;
            ModerationStatus newStatus;

            // 3. Décider du nouveau statut
            if (score < MAX_SAFE_SCORE) {
                newStatus = ModerationStatus.APPROVED; //
            } else {
                newStatus = ModerationStatus.PENDING_REVIEW; // L'IA n'est pas sûre, un admin doit voir
            }

            // 4. Mettre à jour l'annonce dans la BDD
            announcementRepository.findById(announcement.getId()).ifPresent(ann -> {
                ann.setModerationScore(score);
                ann.setModerationStatus(newStatus);
                announcementRepository.save(ann);
                System.out.println("Analyse terminée. Statut mis à jour pour l'annonce ID: " + ann.getId());
            });

        } catch (IOException e) {
            System.err.println("Échec de l'analyse asynchrone pour l'annonce ID: " + announcement.getId() + " - " + e.getMessage());
        }
    }

    /**
     * Modifie une annonce (RDT-6)
     */
    public Announcement updateAnnouncement(
            Long announcementId,
            String userEmail,
            AnnouncementRequest request,
            MultipartFile imageFile // L'image est optionnelle
    ) throws IOException {

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        // Vérification de sécurité : l'utilisateur est-il le propriétaire ?
        if (!announcement.getDonor().getId().equals(user.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier cette annonce.");
        }

        // Mettre à jour les champs depuis le DTO (s'il est fourni)
        if (request != null) {
            announcement.setTitle(request.getTitle());
            announcement.setDescription(request.getDescription());
            announcement.setType(request.getType());
            announcement.setQuantity(request.getQuantity());
            announcement.setFoodType(request.getFoodType());
            announcement.setExpiryDate(request.getExpiryDate());
            announcement.setPickupTime(request.getPickupTime());
            announcement.setAddress(request.getAddress());
        }

        // L'annonce repasse en PENDING_REVIEW lors d'une modification
        announcement.setModerationStatus(ModerationStatus.PENDING_REVIEW);

        // Gérer la nouvelle image (si fournie)
        if (imageFile != null && !imageFile.isEmpty()) {
            // (TODO: Pensez à supprimer l'ancien fichier image du disque)
            // fileStorageService.deleteFile(announcement.getImageUrl(), "announcements");

            String newImageUrl = fileStorageService.saveFile(imageFile, "announcements");
            announcement.setImageUrl(newImageUrl);

            // Relancer l'analyse asynchrone
            processAnnouncementModeration(announcement, imageFile);
        }

        return announcementRepository.save(announcement);
    }

    /**
     * Supprime une annonce (RDT-7)
     */
    public void deleteAnnouncement(Long announcementId, String userEmail) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Annonce non trouvée"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        // Vérification de sécurité
        if (!announcement.getDonor().getId().equals(user.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer cette annonce.");
        }

        // (TODO: Supprimer le fichier image du disque)
        // fileStorageService.deleteFile(announcement.getImageUrl(), "announcements");

        announcementRepository.delete(announcement);
    }
}