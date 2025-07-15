package tn.esprit.forumservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Service
public class ForumService {

    @Value("${cephfs.mount.path:/mnt/cephfs}")
    private String cephfsBasePath;

    public void saveMessage(String message) {
        try {
            Path forumDir = Paths.get(cephfsBasePath, "forum");
            Files.createDirectories(forumDir);

            String filename = "msg-" + System.currentTimeMillis() + ".txt";
            Path filePath = forumDir.resolve(filename);

            Files.writeString(filePath, message);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du message : " + e.getMessage());
        }
    }
}
