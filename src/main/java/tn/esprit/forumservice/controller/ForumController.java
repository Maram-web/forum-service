package com.example.forum.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    @Value("${cephfs.mount.path:/mnt/cephfs/forum}")
    private String cephFsPath;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(cephFsPath));
            Path path = Paths.get(cephFsPath, file.getOriginalFilename());
            Files.write(path, file.getBytes());
            return ResponseEntity.ok("✅ Fichier déposé dans CephFS !");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("❌ Erreur : " + e.getMessage());
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles() {
        try (Stream<Path> stream = Files.list(Paths.get(cephFsPath))) {
            List<String> files = stream.map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(List.of("❌ Erreur lecture fichiers"));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("filename") String filename) {
        try {
            Path path = Paths.get(cephFsPath, filename);
            byte[] file = Files.readAllBytes(path);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .body(file);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
