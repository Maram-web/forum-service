package tn.esprit.forumservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Question {
    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private String content;
    private String author;
    private boolean resolved = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<Answer> answers = new ArrayList<>();
}
