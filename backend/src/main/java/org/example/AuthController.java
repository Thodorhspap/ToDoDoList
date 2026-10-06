package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Επιτρέπει στο Nuxt να επικοινωνεί με την Java
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private CommunityTaskRepository communityTaskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        if (isBlank(user.getUsername()) || isBlank(user.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Συμπλήρωσε όνομα και κωδικό"));
        }

        User foundUser = userRepository.findByUsername(user.getUsername().trim());

        if (foundUser == null || !passwordMatches(user.getPassword(), foundUser)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Λάθος στοιχεία σύνδεσης"));
        }

        // Στέλνουμε πίσω το ID και το όνομα του χρήστη
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Επιτυχής σύνδεση!",
                "userId", foundUser.getId(),
                "username", foundUser.getUsername()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (isBlank(user.getUsername()) || isBlank(user.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Συμπλήρωσε όνομα και κωδικό"));
        }

        user.setUsername(user.getUsername().trim());
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Ο χρήστης υπάρχει ήδη"));
        }

        // Αποθηκεύουμε μόνο το hash του κωδικού, ποτέ τον ίδιο τον κωδικό
        user.setId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        // Στέλνουμε πίσω το ID και το όνομα του νέου χρήστη
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Επιτυχής εγγραφή!",
                "userId", savedUser.getId(),
                "username", savedUser.getUsername()
        ));
    }

    // GET: Λίστα με τα ονόματα όλων των χρηστών (για να διαλέξεις σε ποιον στέλνεις task)
    @GetMapping("/users")
    public List<String> listUsernames() {
        return userRepository.findAll().stream()
                .map(User::getUsername)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    // PUT: Αλλαγή κωδικού. Χρειάζεται τον τρέχοντα κωδικό για επιβεβαίωση.
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (isBlank(username) || isBlank(currentPassword) || isBlank(newPassword)) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Συμπλήρωσε όλα τα πεδία"));
        }
        if (newPassword.length() < 4) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Ο νέος κωδικός πρέπει να έχει τουλάχιστον 4 χαρακτήρες"));
        }

        User foundUser = userRepository.findByUsername(username.trim());
        if (foundUser == null || !passwordMatches(currentPassword, foundUser)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Ο τρέχων κωδικός είναι λάθος"));
        }

        foundUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(foundUser);

        return ResponseEntity.ok(Map.of("success", true, "message", "Ο κωδικός άλλαξε!"));
    }

    // PUT: Αλλαγή ονόματος. Τα tasks είναι δεμένα με το όνομα, οπότε μεταφέρονται κι αυτά.
    @PutMapping("/username")
    @Transactional
    public ResponseEntity<?> changeUsername(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String newUsername = body.get("newUsername");

        if (isBlank(username) || isBlank(password) || isBlank(newUsername)) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Συμπλήρωσε όλα τα πεδία"));
        }

        newUsername = newUsername.trim();
        User foundUser = userRepository.findByUsername(username.trim());
        if (foundUser == null || !passwordMatches(password, foundUser)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Ο κωδικός είναι λάθος"));
        }
        if (newUsername.equals(foundUser.getUsername())) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Αυτό είναι ήδη το όνομά σου"));
        }
        if (userRepository.existsByUsername(newUsername)) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Το όνομα χρησιμοποιείται ήδη"));
        }

        String oldUsername = foundUser.getUsername();
        foundUser.setUsername(newUsername);
        userRepository.save(foundUser);
        taskRepository.moveTasks(oldUsername, newUsername);
        taskRepository.renameSender(oldUsername, newUsername);
        friendshipRepository.renameRequester(oldUsername, newUsername);
        friendshipRepository.renameAddressee(oldUsername, newUsername);
        messageRepository.renameSender(oldUsername, newUsername);
        messageRepository.renameRecipient(oldUsername, newUsername);
        communityTaskRepository.renamePoster(oldUsername, newUsername);
        communityTaskRepository.renameCompleter(oldUsername, newUsername);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Το όνομα άλλαξε!",
                "username", newUsername
        ));
    }

    // Ελέγχει τον κωδικό. Παλιοί λογαριασμοί με απλό κείμενο αναβαθμίζονται σε BCrypt στην πρώτη σύνδεση.
    private boolean passwordMatches(String rawPassword, User foundUser) {
        String stored = foundUser.getPassword();
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        if (stored.equals(rawPassword)) {
            foundUser.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(foundUser);
            return true;
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
