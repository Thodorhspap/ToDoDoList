package org.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // Η συνομιλία δύο χρηστών, μόνο τα μηνύματα μετά το afterId (για να φέρνουμε μόνο τα καινούρια)
    @Query("SELECT m FROM Message m WHERE ((m.sender = :a AND m.recipient = :b) OR (m.sender = :b AND m.recipient = :a)) AND m.id > :afterId ORDER BY m.id")
    List<Message> findConversation(@Param("a") String a, @Param("b") String b, @Param("afterId") Long afterId);

    // Αδιάβαστα μηνύματα ανά αποστολέα: [sender, πλήθος]
    @Query("SELECT m.sender, COUNT(m) FROM Message m WHERE m.recipient = :username AND m.seen = false GROUP BY m.sender")
    List<Object[]> countUnreadBySender(@Param("username") String username);

    // Σημειώνει ως διαβασμένα τα μηνύματα που μας έστειλε ένας φίλος
    @Modifying
    @Query("UPDATE Message m SET m.seen = true WHERE m.recipient = :username AND m.sender = :friend AND m.seen = false")
    int markRead(@Param("username") String username, @Param("friend") String friend);

    // Το τελευταίο μήνυμα από a προς b που έχει διαβάσει ο b (για το "Seen")
    @Query("SELECT MAX(m.id) FROM Message m WHERE m.sender = :a AND m.recipient = :b AND m.seen = true")
    Long lastSeenId(@Param("a") String a, @Param("b") String b);

    // Ενημέρωση ονομάτων όταν κάποιος αλλάζει username
    @Modifying
    @Query("UPDATE Message m SET m.sender = :newName WHERE m.sender = :oldName")
    int renameSender(@Param("oldName") String oldName, @Param("newName") String newName);

    @Modifying
    @Query("UPDATE Message m SET m.recipient = :newName WHERE m.recipient = :oldName")
    int renameRecipient(@Param("oldName") String oldName, @Param("newName") String newName);
}
