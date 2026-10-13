package org.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

// Ένα βήμα (checklist item) μέσα σε ένα task
@Entity
@Table(name = "subtasks")
public class Subtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Το task στο οποίο ανήκει (δεν το στέλνουμε στο JSON, γιατί το task ήδη περιέχει τα βήματά του)
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false, length = 200)
    private String text;

    @Column(nullable = false)
    private Boolean done = false;

    // Η σειρά εμφάνισης μέσα στο task
    @Column(nullable = false)
    private Integer position = 0;

    // --- Getters και Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Boolean getDone() { return done; }
    public void setDone(Boolean done) { this.done = done; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
