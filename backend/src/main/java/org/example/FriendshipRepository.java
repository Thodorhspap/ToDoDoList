package org.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    // Όλες οι φιλίες και τα αιτήματα όπου συμμετέχει ο χρήστης
    List<Friendship> findByRequesterOrAddressee(String requester, String addressee);

    // Η σχέση ανάμεσα σε δύο χρήστες, όποιος κι αν έστειλε το αίτημα
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :a AND f.addressee = :b) OR (f.requester = :b AND f.addressee = :a)")
    List<Friendship> findBetween(@Param("a") String a, @Param("b") String b);

    // Πόσα αιτήματα φιλίας περιμένουν απάντηση από τον χρήστη
    long countByAddresseeAndStatus(String addressee, String status);

    // Ενημέρωση ονομάτων όταν κάποιος αλλάζει username
    @Modifying
    @Query("UPDATE Friendship f SET f.requester = :newName WHERE f.requester = :oldName")
    int renameRequester(@Param("oldName") String oldName, @Param("newName") String newName);

    @Modifying
    @Query("UPDATE Friendship f SET f.addressee = :newName WHERE f.addressee = :oldName")
    int renameAddressee(@Param("oldName") String oldName, @Param("newName") String newName);
}
