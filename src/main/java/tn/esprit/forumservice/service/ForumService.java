package tn.esprit.forumservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Logger;

@Service
public class ForumService {

    private static final Logger LOGGER = Logger.getLogger(ForumService.class.getName());

    @Value("${cephfs.mount.path:/mnt/cephfs}")
    private String cephfsBasePath;

    public void saveMessage(String message) {
        try {
            Path forumDir = Paths.get(cephfsBasePath, "forum");

            if (!Files.exists(forumDir)) {
                LOGGER.info("Le répertoire forum n'existe pas, tentative de création...");
                Files.createDirectories(forumDir);
            }

            String filename = "msg-" + System.currentTimeMillis() + ".txt";
            Path filePath = forumDir.resolve(filename);

            LOGGER.info("Écriture du message dans le fichier : " + filePath);
            Files.writeString(filePath, message);

            LOGGER.info("Message sauvegardé avec succès : " + filename);
        } catch (IOException e) {
            LOGGER.severe("Erreur lors de l'enregistrement du message : " + e.getMessage());
            e.printStackTrace(); // important pour voir dans les logs du pod
            throw new RuntimeException("Erreur lors de l'enregistrement du message : " + e.getMessage());
        }
    }
}
