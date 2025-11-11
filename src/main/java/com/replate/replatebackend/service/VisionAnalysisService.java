package com.replate.replatebackend.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;

@Service
public class VisionAnalysisService {

    /**
     * Analyse une image pour la modération de contenu (Safe Search).
     * C'est l'implémentation de "AiService".
     * * @param file Le fichier image uploadé
     * @return Un score de "danger". Un score élevé signifie un contenu inapproprié.
     * @throws IOException
     */
    public double analyzeImageForSafety(MultipartFile file) throws IOException {

        // ImageAnnotatorClient.create() trouve automatiquement
        // la variable d'environnement GOOGLE_APPLICATION_CREDENTIALS
        // que vous avez configurée dans IntelliJ.
        try (ImageAnnotatorClient visionClient = ImageAnnotatorClient.create()) {

            // Convertit le fichier en un format que l'API comprend
            ByteString imgBytes = ByteString.copyFrom(file.getBytes());
            Image img = Image.newBuilder().setContent(imgBytes).build();

            // Demande la fonctionnalité "SAFE_SEARCH_DETECTION"
            Feature feature = Feature.newBuilder().setType(Feature.Type.SAFE_SEARCH_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(img)
                    .build();

            // Envoie la requête à Google
            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(
                    Collections.singletonList(request));
            AnnotateImageResponse res = response.getResponsesList().get(0);

            if (res.hasError()) {
                throw new RuntimeException("Erreur de l'API Vision: " + res.getError().getMessage());
            }

            // Récupère les résultats
            SafeSearchAnnotation annotation = res.getSafeSearchAnnotation();

            // Calcule un "score de danger" basé sur les résultats de Google
            // Cela correspond à "analyseResult(result, score)"
            double score = 0;
            score += getLikelihoodScore(annotation.getAdult()); // Contenu pour adultes
            score += getLikelihoodScore(annotation.getViolence()); // Violence
            score += getLikelihoodScore(annotation.getRacy()); // Contenu osé

            System.out.println("Score de modération de l'image : " + score);
            return score;
        }
    }

    /**
     * Utilitaire pour convertir la réponse de Google (ex: "VERY_LIKELY")
     * en un score numérique que nous pouvons utiliser.
     */
    private int getLikelihoodScore(Likelihood likelihood) {
        switch (likelihood) {
            case VERY_LIKELY: return 5;
            case LIKELY: return 4;
            case POSSIBLE: return 3;
            case UNLIKELY: return 1;
            case VERY_UNLIKELY: return 0;
            default: return 0; // (Pour UNKNOWN)
        }
    }
}