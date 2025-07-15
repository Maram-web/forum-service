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

    // ✅ Publier un message (authentifié)
    @PostMapping("/post")
    public ResponseEntity<String> postMessage(@RequestBody Map<String, String> body,
                                              Authentication authentication) {
        String username = authentication.getName();
        String message = body.get("message");
        String entry = "[" + username + "] " + message;

        forumService.saveMessage(entry);
        return ResponseEntity.ok("✅ Message posté");
    }

    // ✅ Récupérer tous les messages
    @GetMapping("/messages")
    public ResponseEntity<List<String>> getMessages() {
        try {
            Path forumDir = getForumDir();
            List<String> messages = new ArrayList<>();

            if (Files.exists(forumDir)) {
                Files.list(forumDir)
                        .filter(Files::isRegularFile)
                        .sorted(Comparator.comparingLong(f -> f.toFile().lastModified()))
                        .forEach(path -> {
                            try {
                                messages.add(Files.readString(path));
                            } catch (IOException ignored) {}
                        });
            }

            return ResponseEntity.ok(messages);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/path")
    public ResponseEntity<String> getMountPath() {
        return ResponseEntity.ok(getForumDir().toString());
    }

    @GetMapping("/me")
    public ResponseEntity<String> getMyUsername(Authentication authentication) {
        return ResponseEntity.ok(authentication.getName());

    }



    @PostMapping("/comment")
    public ResponseEntity<String> postComment(@RequestParam String postFile,
                                              @RequestParam String comment,
                                              Authentication auth) {
        String username = auth.getName();

        Path postPath = getForumDir().resolve(postFile);
        Path commentsDir = Paths.get(postPath.toString() + ".comments");

        try {
            Files.createDirectories(commentsDir);
            String filename = "comment-" + System.currentTimeMillis() + "-" + username + ".txt";
            Files.writeString(commentsDir.resolve(filename), "[" + username + "] " + comment);
            return ResponseEntity.ok("✅ Commentaire ajouté");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("❌ Erreur : " + e.getMessage());
        }
    }




    @GetMapping("/messages")
    public ResponseEntity<List<ForumPost>> getMessagesAndComments() {
        List<ForumPost> results = new ArrayList<>();
        try {
            Path forumDir = getForumDir();
            if (!Files.exists(forumDir)) return ResponseEntity.ok(results);

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





}
