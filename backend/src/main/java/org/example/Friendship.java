package org.example;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "friendships")
public class Friendship {

    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ποιος έστειλε το αίτημα φιλίας
    @Column(nullable = false)
    private String requester;

    // Σε ποιον το έστειλε
    @Column(nullable = false)
    private String addressee;

    // PENDING μέχρι να το αποδεχτεί, μετά ACCEPTED
    @Column(nullable = false)
    private String status = PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Getters και Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequester() { return requester; }
    public void setRequester(String requester) { this.requester = requester; }

    public String getAddressee() { return addressee; }
    public void setAddressee(String addressee) { this.addressee = addressee; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Το όνομα του "άλλου" ατόμου, από τη ματιά του username
    public String otherThan(String username) {
        return requester.equals(username) ? addressee : requester;
    }
}
