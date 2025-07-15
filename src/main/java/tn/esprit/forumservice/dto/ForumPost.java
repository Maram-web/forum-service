package tn.esprit.forumservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPost {
    private String post;
    private String filename;
    private String date;
    private List<String> comments;
}
