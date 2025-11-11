package com.replate.replatebackend.repository;

import com.replate.replatebackend.model.Announcement;
import com.replate.replatebackend.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Interface Repository pour l'entité Announcement.
 * JpaRepository nous donne toutes les méthodes de base (save, findById, delete, etc.)
 */
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * Méthode personnalisée pour trouver toutes les annonces
     * postées par un utilisateur spécifique (Merchant ou Association).
     */
    List<Announcement> findByDonor(Merchant donor);


}