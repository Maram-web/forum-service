package tn.esprit.forumservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.forumservice.dto.ForumPost;
import tn.esprit.forumservice.service.ForumService;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/forum")
@Slf4j  // <<--- Ajoute l’annotation pour activer le logger SLF4J
public class ForumController {

    @Value("${cephfs.mount.path:/mnt/cephfs}")
    private String cephfsBasePath;

    private final ForumService forumService;

    private Path getForumDir() {
        return Paths.get(cephfsBasePath, "forum");
    }

    @PostMapping("/post")
    public ResponseEntity<String> postMessage(@RequestBody Map<String, String> body, Authentication authentication) {
        try {
            String username = authentication.getName();
            String message = body.get("message");
            String entry = "[" + username + "] " + message;

            log.info("📩 [{}] poste un message : {}", username, message);
            forumService.saveMessage(entry);
            return ResponseEntity.ok("✅ Message posté");
        } catch (Exception e) {
            log.error("❌ Erreur lors de la publication du message : {}", e.getMessage());
            return ResponseEntity.internalServerError().body("❌ Erreur lors de la publication du message : " + e.getMessage());
        }
    }

    @PostMapping("/comment")
    public ResponseEntity<String> postComment(@RequestParam String postFile,
                                              @RequestParam String comment,
                                              Authentication auth) {
        try {
            String username = auth.getName();
            Path postPath = getForumDir().resolve(postFile);
            Path commentsDir = Paths.get(postPath.toString() + ".comments");

            if (!Files.exists(postPath)) {
                log.warn("📄 Tentative de commenter un post inexistant : {}", postFile);
                return ResponseEntity.badRequest().body("❌ Le post n'existe pas !");
            }

            Files.createDirectories(commentsDir);
            String filename = "comment-" + System.currentTimeMillis() + "-" + username + ".txt";
            Files.writeString(commentsDir.resolve(filename), "[" + username + "] " + comment);

            log.info("💬 [{}] commente le post {} : {}", username, postFile, comment);
            return ResponseEntity.ok("✅ Commentaire ajouté");
        } catch (IOException e) {
            log.error("❌ Erreur lors de l'ajout du commentaire : {}", e.getMessage());
            return ResponseEntity.internalServerError().body("❌ Erreur lors de l'ajout du commentaire : " + e.getMessage());
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<List<ForumPost>> getMessagesAndComments() {
        List<ForumPost> results = new ArrayList<>();
        try {
            Path forumDir = getForumDir();
            if (!Files.exists(forumDir)) {
                log.info("📂 Aucun dossier de messages trouvé, retourne une liste vide.");
                return ResponseEntity.ok(results);
            }

            Files.list(forumDir)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .forEach(postPath -> {
                        try {
                            String post = Files.readString(postPath);
                            String filename = postPath.getFileName().toString();
                            String date = new Date(postPath.toFile().lastModified()).toString();

                            List<String> comments = new ArrayList<>();
                            Path commentsDir = Paths.get(postPath.toString() + ".comments");

                            if (Files.exists(commentsDir)) {
                                Files.list(commentsDir)
                                        .sorted(Comparator.comparingLong(c -> c.toFile().lastModified()))
                                        .forEach(cPath -> {
                                            try {
                                                comments.add(Files.readString(cPath));
                                            } catch (IOException ignored) {
                                                log.warn("⚠️ Impossible de lire un commentaire : {}", cPath);
                                            }
                                        });
                            }

                            results.add(new ForumPost(post, filename, date, comments));
                        } catch (IOException e) {
                            log.warn("⚠️ Impossible de lire un post : {}", postPath);
                        }
                    });

            log.info("📨 {} posts récupérés avec leurs commentaires", results.size());
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            log.error("❌ Erreur lors de la récupération des messages : {}", e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/path")
    public ResponseEntity<String> getMountPath() {
        String path = getForumDir().toString();
        log.info("📁 Chemin du montage CephFS : {}", path);
        return ResponseEntity.ok(path);
    }

    @GetMapping("/me")
    public ResponseEntity<String> getMyUsername(Authentication authentication) {
        String username = authentication.getName();
        log.info("🙋 Utilisateur authentifié : {}", username);
        return ResponseEntity.ok(username);
    }
}
