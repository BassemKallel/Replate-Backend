package com.replate.replatebackend.service;

import com.google.common.io.Files;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource; // <-- NOUVEL IMPORT
import org.springframework.core.io.UrlResource; // <-- NOUVEL IMPORT
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException; // <-- NOUVEL IMPORT
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootLocation;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
        try {
            java.nio.file.Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Ne peut pas initialiser le dossier de stockage", e);
        }
    }

    /**
     * CREATE: Sauvegarde un fichier sur le disque.
     */
    public String saveFile(MultipartFile file, String subFolder) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("Impossible de stocker un fichier vide.");
        }
        Path subDirLocation = this.rootLocation.resolve(subFolder);
        java.nio.file.Files.createDirectories(subDirLocation);

        String extension = com.google.common.io.Files.getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString() + "." + extension;

        Path destinationFile = subDirLocation.resolve(uniqueFileName).normalize().toAbsolutePath();
        try (InputStream inputStream = file.getInputStream()) {
            java.nio.file.Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return uniqueFileName;
    }

    /**
     * READ: Charge un fichier depuis le disque.
     * @param filename Le nom unique du fichier
     * @param subFolder Le sous-dossier (ex: "users" ou "announcements")
     * @return Une ressource (Resource) que le contrôleur peut envoyer
     */
    public Resource loadFile(String filename, String subFolder) {
        try {
            Path file = rootLocation.resolve(subFolder).resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Impossible de lire le fichier: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier: " + filename, e);
        }
    }

    /**
     * DELETE: Supprime un fichier du disque.
     * @param filename Le nom unique du fichier
     * @param subFolder Le sous-dossier
     */
    public void deleteFile(String filename, String subFolder) {
        try {
            Path file = rootLocation.resolve(subFolder).resolve(filename);
            java.nio.file.Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la suppression du fichier: " + filename, e);
        }
    }
}