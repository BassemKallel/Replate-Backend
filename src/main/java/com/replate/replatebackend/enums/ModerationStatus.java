package com.replate.replatebackend.enums;

/**
 * Définit le statut de modération d'une annonce.
 * Basé sur votre diagramme UML.
 */
public enum ModerationStatus {
    PENDING_REVIEW, // En attente (statut par défaut lors de la création)
    APPROVED, // Approuvé (par l'IA ou un admin)
    REJECTED // Rejeté
}