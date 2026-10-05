package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    // GET: Φίλοι, αιτήματα που λάβαμε και αιτήματα που στείλαμε
    @GetMapping("/{username}")
    public Map<String, Object> getFriends(@PathVariable String username) {
        List<String> friends = new ArrayList<>();
        List<Map<String, Object>> incoming = new ArrayList<>();
        List<Map<String, Object>> outgoing = new ArrayList<>();

        for (Friendship f : friendshipRepository.findByRequesterOrAddressee(username, username)) {
            if (Friendship.ACCEPTED.equals(f.getStatus())) {
                friends.add(f.otherThan(username));
            } else if (f.getAddressee().equals(username)) {
                incoming.add(Map.of("id", f.getId(), "username", f.getRequester()));
            } else {
                outgoing.add(Map.of("id", f.getId(), "username", f.getAddressee()));
            }
        }
        friends.sort(String.CASE_INSENSITIVE_ORDER);

        return Map.of("friends", friends, "incoming", incoming, "outgoing", outgoing);
    }

    // POST: Στέλνει αίτημα φιλίας. Αν ο άλλος μας έχει ήδη στείλει, γινόμαστε φίλοι αμέσως.
    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, String> body) {
        String from = trim(body.get("from"));
        String to = trim(body.get("to"));

        if (from.isEmpty() || to.isEmpty()) {
            return error(400, "Συμπλήρωσε όνομα χρήστη");
        }
        if (from.equals(to)) {
            return error(400, "Δεν μπορείς να προσθέσεις τον εαυτό σου");
        }
        if (!userRepository.existsByUsername(to)) {
            return error(404, "Ο χρήστης \"" + to + "\" δεν υπάρχει");
        }

        List<Friendship> existing = friendshipRepository.findBetween(from, to);
        if (!existing.isEmpty()) {
            Friendship f = existing.get(0);
            if (Friendship.ACCEPTED.equals(f.getStatus())) {
                return error(400, "Είστε ήδη φίλοι");
            }
            if (f.getRequester().equals(from)) {
                return error(400, "Έχεις ήδη στείλει αίτημα");
            }
            // Ο άλλος μας είχε στείλει αίτημα, οπότε το αποδεχόμαστε
            f.setStatus(Friendship.ACCEPTED);
            friendshipRepository.save(f);
            return ResponseEntity.ok(Map.of("success", true, "message", "Είστε πλέον φίλοι με " + to + "!"));
        }

        Friendship f = new Friendship();
        f.setRequester(from);
        f.setAddressee(to);
        friendshipRepository.save(f);
        return ResponseEntity.ok(Map.of("success", true, "message", "Το αίτημα στάλθηκε στον " + to + "!"));
    }

    // POST: Αποδοχή αιτήματος (μόνο από αυτόν που το έλαβε)
    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Friendship f = friendshipRepository.findById(id).orElse(null);
        if (f == null || !Friendship.PENDING.equals(f.getStatus()) || !f.getAddressee().equals(trim(body.get("username")))) {
            return error(404, "Το αίτημα δεν βρέθηκε");
        }
        f.setStatus(Friendship.ACCEPTED);
        friendshipRepository.save(f);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // DELETE: Απόρριψη αιτήματος που λάβαμε ή ακύρωση αιτήματος που στείλαμε
    @DeleteMapping("/requests/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id, @RequestParam String username) {
        Friendship f = friendshipRepository.findById(id).orElse(null);
        if (f == null || !Friendship.PENDING.equals(f.getStatus())
                || !(f.getAddressee().equals(username) || f.getRequester().equals(username))) {
            return error(404, "Το αίτημα δεν βρέθηκε");
        }
        friendshipRepository.delete(f);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // DELETE: Αφαίρεση φίλου από τη λίστα
    @DeleteMapping("/{username}/{friend}")
    public ResponseEntity<?> removeFriend(@PathVariable String username, @PathVariable String friend) {
        List<Friendship> existing = friendshipRepository.findBetween(username, friend);
        if (existing.isEmpty()) {
            return error(404, "Δεν είστε φίλοι");
        }
        friendshipRepository.deleteAll(existing);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // GET: Μηνύματα με έναν φίλο (afterId για να φέρνουμε μόνο τα καινούρια).
    // Όταν τα φέρνουμε, τα μηνύματα του φίλου σημειώνονται ως διαβασμένα.
    @GetMapping("/{username}/messages/{friend}")
    @Transactional
    public ResponseEntity<?> getMessages(@PathVariable String username, @PathVariable String friend,
                                         @RequestParam(defaultValue = "0") Long afterId) {
        if (!areFriends(username, friend)) {
            return error(403, "Μπορείς να μιλάς μόνο με φίλους");
        }
        List<Message> messages = messageRepository.findConversation(username, friend, afterId);
        messageRepository.markRead(username, friend);

        // Μαζί στέλνουμε και μέχρι ποιο δικό μας μήνυμα έχει διαβάσει ο φίλος
        Long lastSeenId = messageRepository.lastSeenId(username, friend);
        return ResponseEntity.ok(Map.of(
                "messages", messages,
                "lastSeenId", lastSeenId == null ? 0L : lastSeenId
        ));
    }

    // POST: Στέλνει μήνυμα σε φίλο
    @PostMapping("/{username}/messages/{friend}")
    public ResponseEntity<?> sendMessage(@PathVariable String username, @PathVariable String friend,
                                         @RequestBody Map<String, String> body) {
        String text = trim(body.get("text"));
        if (text.isEmpty()) {
            return error(400, "Το μήνυμα είναι κενό");
        }
        if (text.length() > MAX_MESSAGE_LENGTH) {
            return error(400, "Το μήνυμα είναι πολύ μεγάλο (μέχρι " + MAX_MESSAGE_LENGTH + " χαρακτήρες)");
        }
        if (!areFriends(username, friend)) {
            return error(403, "Μπορείς να μιλάς μόνο με φίλους");
        }

        Message message = new Message();
        message.setSender(username);
        message.setRecipient(friend);
        message.setText(text);
        return ResponseEntity.ok(messageRepository.save(message));
    }

    private boolean areFriends(String a, String b) {
        return friendshipRepository.findBetween(a, b).stream()
                .anyMatch(f -> Friendship.ACCEPTED.equals(f.getStatus()));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
