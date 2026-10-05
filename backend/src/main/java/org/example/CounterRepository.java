package org.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CounterRepository extends JpaRepository<Counter, String> {
    // Αυξάνει τον μετρητή απευθείας στη βάση, ώστε ταυτόχρονα κλικ να μη χάνονται
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Counter c SET c.value = c.value + 1 WHERE c.name = :name")
    int increment(@Param("name") String name);
}
