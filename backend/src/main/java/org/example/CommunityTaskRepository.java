package org.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommunityTaskRepository extends JpaRepository<CommunityTask, Long> {

    // Ο τοίχος: τα ανοιχτά tasks και όσα ολοκληρώθηκαν μετά από μια ημερομηνία, από το πιο πρόσφατο
    @Query("SELECT t FROM CommunityTask t WHERE t.completedBy IS NULL OR t.completedAt > :since ORDER BY t.id DESC")
    List<CommunityTask> findWall(@Param("since") LocalDateTime since);

    // Ολοκληρώνει το task μόνο αν είναι ακόμα ανοιχτό (αν δύο χρήστες πατήσουν μαζί, κερδίζει ο πρώτος)
    // clearAutomatically: για να δούμε μετά την ενημερωμένη εγγραφή και όχι την παλιά από την cache
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CommunityTask t SET t.completedBy = :username, t.completedAt = :now, t.completionSeen = false " +
            "WHERE t.id = :id AND t.completedBy IS NULL")
    int complete(@Param("id") Long id, @Param("username") String username, @Param("now") LocalDateTime now);

    // Πόσα από τα tasks που ανέβασε ο χρήστης ολοκληρώθηκαν χωρίς να το έχει δει
    long countByPostedByAndCompletionSeenFalse(String postedBy);

    // Σημειώνει ότι ο χρήστης είδε τις ολοκληρώσεις των tasks του
    @Modifying
    @Query("UPDATE CommunityTask t SET t.completionSeen = true WHERE t.postedBy = :postedBy AND t.completionSeen = false")
    int markCompletionsSeen(@Param("postedBy") String postedBy);

    // Αλλαγή username: ενημερώνουμε και τα δύο ονόματα
    @Modifying
    @Query("UPDATE CommunityTask t SET t.postedBy = :newName WHERE t.postedBy = :oldName")
    int renamePoster(@Param("oldName") String oldName, @Param("newName") String newName);

    @Modifying
    @Query("UPDATE CommunityTask t SET t.completedBy = :newName WHERE t.completedBy = :oldName")
    int renameCompleter(@Param("oldName") String oldName, @Param("newName") String newName);
}
