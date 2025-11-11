package com.replate.replatebackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Service pour analyser le contenu des images via Cloudinary/AWS Rekognition.
 * Implémente l'analyse IA asynchrone.
 */
@Service
public class CloudinaryAnalysisService {

    private final Cloudinary cloudinary;

    /**
     * Le constructeur configure Cloudinary avec les valeurs de application.properties.
     */
    public CloudinaryAnalysisService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    /**
     * Analyse une image uploadée pour la modération de contenu.
     * Le fichier est uploadé temporairement pour l'analyse, puis détruit immédiatement.
     * @param file Le fichier image à analyser
     * @return Un score de "danger" (0.0=sûr, 10.0=dangereux)
     */
    public double analyzeImageForSafety(MultipartFile file) throws IOException {

        // 1. Cloudinary a besoin d'un objet java.io.File, on convertit le MultipartFile.
        File tempFile = convertMultiPartToFile(file);

        try {
            // 2. Upload sur Cloudinary avec analyse de contenu explicite
            // L'option "moderation" active l'add-on de modération (ex: AWS Rekognition).
            Map uploadResult = cloudinary.uploader().upload(tempFile,
                    ObjectUtils.asMap(
                            "moderation", "aws_rek" // Requiert l'add-on Rekognition sur le compte Cloudinary
                    )
            );

            // 3. Récupère le statut de modération
            String moderationStatus = (String) uploadResult.get("moderation_status");
            String publicId = (String) uploadResult.get("public_id");

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            if ("rejected".equalsIgnoreCase(moderationStatus)) {
                return 10.0; // Non conforme (score très élevé)
            } else if ("pending".equalsIgnoreCase(moderationStatus)) {
                return 6.0; // Nécessite une revue manuelle (score moyen)
            } else {
                return 0.0; // Approuvé par l'IA (score de sécurité maximal)
            }

        } catch (Exception e) {
            System.err.println("Erreur d'analyse Cloudinary: " + e.getMessage());
            return 7.0;
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Utilitaire pour convertir un MultipartFile en java.io.File.
     */
    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir"), file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}