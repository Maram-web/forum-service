package tn.esprit.forumservice.controller;

import lombok.RequiredArgsConstructor;
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
@RequestMapping("/forum")
public class ForumController {

    @Value("${cephfs.mount.path:/mnt/cephfs}")
    private String cephfsBasePath;

    private final ForumService forumService;

    private Path getForumDir() {
        return Paths.get(cephfsBasePath, "forum");
    }

    // ✅ Publier un message
    @PostMapping("/post")
    public ResponseEntity<String> postMessage(@RequestBody Map<String, String> body, Authentication authentication) {
        try {
            String username = authentication.getName();
            String message = body.get("message");
            String entry = "[" + username + "] " + message;
            forumService.saveMessage(entry);
            return ResponseEntity.ok("✅ Message posté");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("❌ Erreur lors de la publication du message : " + e.getMessage());
        }
    }

    // ✅ Ajouter un commentaire à un post
    @PostMapping("/comment")
    public ResponseEntity<String> postComment(@RequestParam String postFile,
                                              @RequestParam String comment,
                                              Authentication auth) {
        try {
            String username = auth.getName();
            Path postPath = getForumDir().resolve(postFile);
            Path commentsDir = Paths.get(postPath.toString() + ".comments");

            if (!Files.exists(postPath)) {
                return ResponseEntity.badRequest().body("❌ Le post n'existe pas !");
            }

            Files.createDirectories(commentsDir);
            String filename = "comment-" + System.currentTimeMillis() + "-" + username + ".txt";
            Files.writeString(commentsDir.resolve(filename), "[" + username + "] " + comment);

            return ResponseEntity.ok("✅ Commentaire ajouté");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("❌ Erreur lors de l'ajout du commentaire : " + e.getMessage());
        }
    }

    // ✅ Récupérer tous les posts et leurs commentaires
    @GetMapping("/messages")
    public ResponseEntity<List<ForumPost>> getMessagesAndComments() {
        List<ForumPost> results = new ArrayList<>();
        try {
            Path forumDir = getForumDir();
            if (!Files.exists(forumDir)) {
                return ResponseEntity.ok(results); // retourne liste vide
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
                                            } catch (IOException ignored) {}
                                        });
                            }

                            results.add(new ForumPost(post, filename, date, comments));
                        } catch (IOException ignored) {}
                    });

            return ResponseEntity.ok(results);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // ✅ Obtenir le chemin monté
    @GetMapping("/path")
    public ResponseEntity<String> getMountPath() {
        return ResponseEntity.ok(getForumDir().toString());
    }

    // ✅ Obtenir le nom de l’utilisateur authentifié
    @GetMapping("/me")
    public ResponseEntity<String> getMyUsername(Authentication authentication) {
        return ResponseEntity.ok(authentication.getName());
    }
}
