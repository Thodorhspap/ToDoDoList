package org.example;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ΝΕΟ: Το πεδίο που "δένει" το task με τον εκάστοτε χρήστη
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String text;

    // Νέο πεδίο περιγραφής (nullable και με τύπο TEXT για μεγάλα κείμενα)
    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String author;

    @Column(name = "done", nullable = false)
    private Boolean done = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime dueDate;

    // Ποιος χρήστης έστειλε το task (null αν το έφτιαξε ο ίδιος ο κάτοχος)
    @Column(name = "sent_by", nullable = true)
    private String sentBy;

    // false για task που μας έστειλαν και δεν έχουμε δει ακόμα (null/true = το έχουμε δει)
    @Column(nullable = true)
    private Boolean seen;

    // Πότε ολοκληρώθηκε (null όσο είναι ανοιχτό), για το ιστορικό
    @Column(nullable = true)
    private LocalDateTime completedAt;

    // false όταν ο φίλος ολοκλήρωσε ένα task που του στείλαμε και δεν το έχουμε δει ακόμα
    @Column(nullable = true)
    private Boolean completionSeen;

    // HIGH, MEDIUM, LOW ή null (χωρίς προτεραιότητα)
    @Column(nullable = true, length = 10)
    private String priority;

    // Ελεύθερη κατηγορία, π.χ. "Σχολείο" ή "Σπίτι"
    @Column(nullable = true, length = 30)
    private String category;

    // --- Getters και Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Boolean getDone() { return done; }
    public void setDone(Boolean done) { this.done = done; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public String getSentBy() { return sentBy; }
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }

    public Boolean getSeen() { return seen; }
    public void setSeen(Boolean seen) { this.seen = seen; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Boolean getCompletionSeen() { return completionSeen; }
    public void setCompletionSeen(Boolean completionSeen) { this.completionSeen = completionSeen; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}