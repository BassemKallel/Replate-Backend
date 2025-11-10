package com.replate.replatebackend.controller;

import com.replate.replatebackend.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files") // Un endpoint dédié pour les fichiers
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * CREATE: Endpoint d'upload (similaire à votre test)
     * Utile pour qu'un utilisateur change son image de profil plus tard.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subFolder") String subFolder)
    {
        try {
            String savedFileName = fileStorageService.saveFile(file, subFolder);
            String message = "Fichier uploadé avec succès: " + savedFileName;
            return ResponseEntity.ok(message);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Échec de l'upload: " + e.getMessage());
        }
    }

    /**
     * READ: Endpoint pour voir/télécharger un fichier
     * ex: GET http://localhost:8081/api/files/users/a1b2c3d4-uuid.png
     */
    @GetMapping("/{subFolder}/{filename:.+}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String subFolder,
            @PathVariable String filename)
    {
        Resource file = fileStorageService.loadFile(filename, subFolder);

        // Détermine le type de contenu (ex: image/png, application/pdf)
        String contentType = "application/octet-stream"; // Type par défaut
        try {
            contentType = java.nio.file.Files.probeContentType(file.getFile().toPath());
        } catch (IOException e) {
            // Gérer l'exception
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    /**
     * DELETE: Endpoint pour supprimer un fichier
     * ex: DELETE http://localhost:8081/api/files/users/a1b2c3d4-uuid.png
     */
    @DeleteMapping("/{subFolder}/{filename:.+}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String subFolder,
            @PathVariable String filename)
    {
        try {
            fileStorageService.deleteFile(filename, subFolder);
            String message = "Fichier supprimé avec succès: " + filename;
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Échec de la suppression: " + e.getMessage());
        }
    }
}