package com.replate.replatebackend.controller;

import com.replate.replatebackend.service.FileStorageService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@AllArgsConstructor
@PreAuthorize("isAuthenticated()") // Seuls les utilisateurs connectés peuvent voir les fichiers
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * READ: Endpoint pour voir/télécharger un fichier
     * ex: GET /api/files/users/image.png
     */
    @GetMapping("/{subFolder}/{filename:.+}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String subFolder,
            @PathVariable String filename)
    {
        Resource file = fileStorageService.loadFile(filename, subFolder);

        String contentType = "application/octet-stream";
        try {
            contentType = java.nio.file.Files.probeContentType(file.getFile().toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        } catch (IOException e) {
            // on garde "application/octet-stream" par défaut
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}