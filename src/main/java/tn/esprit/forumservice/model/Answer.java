package tn.esprit.forumservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Answer {
    @Id @GeneratedValue
    private Long id;

    private String content;
    private String author;
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;
}
