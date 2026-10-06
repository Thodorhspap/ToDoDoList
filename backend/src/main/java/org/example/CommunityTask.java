package org.example;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Task στον κοινό τοίχο: το ανεβάζει ένας χρήστης και μπορεί να το ολοκληρώσει οποιοσδήποτε άλλος
@Entity
@Table(name = "community_tasks")
public class CommunityTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String text;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;

    // Ποιος το ανέβασε στον τοίχο
    @Column(name = "posted_by", nullable = false)
    private String postedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // HIGH, MEDIUM, LOW ή null (χωρίς προτεραιότητα)
    @Column(nullable = true, length = 10)
    private String priority;

    @Column(nullable = true, length = 30)
    private String category;

    // Ποιος το ολοκλήρωσε (null όσο είναι ανοιχτό)
    @Column(name = "completed_by", nullable = true)
    private String completedBy;

    @Column(nullable = true)
    private LocalDateTime completedAt;

    // false όταν κάποιος ολοκλήρωσε το task και ο χρήστης που το ανέβασε δεν το έχει δει ακόμα
    @Column(nullable = true)
    private Boolean completionSeen;

    // --- Getters και Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Boolean getCompletionSeen() { return completionSeen; }
    public void setCompletionSeen(Boolean completionSeen) { this.completionSeen = completionSeen; }
}
