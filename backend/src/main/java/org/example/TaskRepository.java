package org.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Βρίσκει όλα τα tasks ταξινομημένα από το πιο πρόσφατο στο πιο παλιό
    List<Task> findAllByOrderByCreatedAtDesc();
    List<Task> findByUserId(String userId);

    // Τα tasks που έστειλε ένας χρήστης σε άλλους, από το πιο πρόσφατο
    List<Task> findBySentByOrderByIdDesc(String sentBy);

    // Μεταφέρει όλα τα tasks ενός χρήστη στο νέο του όνομα (όταν αλλάζει username)
    @Modifying
    @Query("UPDATE Task t SET t.userId = :newUserId WHERE t.userId = :oldUserId")
    int moveTasks(@Param("oldUserId") String oldUserId, @Param("newUserId") String newUserId);

    // Πόσα tasks μας έστειλαν που δεν έχουμε δει ακόμα
    long countByUserIdAndSeenFalse(String userId);

    // Σημειώνει όλα τα tasks του χρήστη ως "τα είδα"
    @Modifying
    @Query("UPDATE Task t SET t.seen = true WHERE t.userId = :userId AND t.seen = false")
    int markSeen(@Param("userId") String userId);

    // Πόσα tasks που στείλαμε ολοκληρώθηκαν χωρίς να το έχουμε δει
    long countBySentByAndDoneTrueAndCompletionSeenFalse(String sentBy);

    // Σημειώνει ότι είδαμε τις ολοκληρώσεις των tasks που στείλαμε
    @Modifying
    @Query("UPDATE Task t SET t.completionSeen = true WHERE t.sentBy = :sentBy AND t.completionSeen = false")
    int markCompletionsSeen(@Param("sentBy") String sentBy);

    // Ενημερώνει και τον αποστολέα στα tasks που έστειλε (όταν αλλάζει username)
    @Modifying
    @Query("UPDATE Task t SET t.sentBy = :newSentBy WHERE t.sentBy = :oldSentBy")
    int renameSender(@Param("oldSentBy") String oldSentBy, @Param("newSentBy") String newSentBy);
}