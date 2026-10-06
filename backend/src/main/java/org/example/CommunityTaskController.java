package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/community")
public class CommunityTaskController {

    private static final Set<String> PRIORITIES = Set.of("HIGH", "MEDIUM", "LOW");
    private static final int MAX_CATEGORY_LENGTH = 30;
    private static final int MAX_TEXT_LENGTH = 255;
    // Πόσες μέρες μένουν στον τοίχο τα ολοκληρωμένα tasks
    private static final int COMPLETED_DAYS = 7;

    @Autowired
    private CommunityTaskRepository communityTaskRepository;

    @Autowired
    private UserRepository userRepository;

    // GET: Ο τοίχος: ανοιχτά tasks και όσα ολοκληρώθηκαν τις τελευταίες μέρες
    @GetMapping
    public List<CommunityTask> getWall() {
        return communityTaskRepository.findWall(LocalDateTime.now().minusDays(COMPLETED_DAYS));
    }

    // POST: Ανεβάζει ένα νέο task στον τοίχο
    @PostMapping
    public ResponseEntity<?> postTask(@RequestBody CommunityTask task) {
        if (task.getPostedBy() == null || !userRepository.existsByUsername(task.getPostedBy())) {
            return error(400, "Πρέπει να είσαι συνδεδεμένος για να ανεβάσεις task");
        }
        if (task.getText() == null || task.getText().isBlank()) {
            return error(400, "Ο τίτλος δεν μπορεί να είναι κενός");
        }
        if (task.getText().trim().length() > MAX_TEXT_LENGTH) {
            return error(400, "Ο τίτλος είναι πολύ μεγάλος (μέχρι " + MAX_TEXT_LENGTH + " χαρακτήρες)");
        }

        task.setId(null);
        task.setText(task.getText().trim());
        task.setCreatedAt(LocalDateTime.now());
        task.setPriority(task.getPriority() != null && PRIORITIES.contains(task.getPriority()) ? task.getPriority() : null);
        task.setCategory(cleanCategory(task.getCategory()));
        task.setCompletedBy(null);
        task.setCompletedAt(null);
        task.setCompletionSeen(null);
        return ResponseEntity.ok(communityTaskRepository.save(task));
    }

    // PUT: Κάποιος (όχι αυτός που το ανέβασε) ολοκληρώνει το task
    @PutMapping("/{taskId}/complete")
    @Transactional
    public ResponseEntity<?> completeTask(@PathVariable Long taskId, @RequestBody Map<String, String> body) {
        String username = body.get("username");
        CommunityTask task = communityTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return error(404, "Το task δεν υπάρχει πια");
        }
        if (username == null || !userRepository.existsByUsername(username)) {
            return error(400, "Πρέπει να είσαι συνδεδεμένος για να ολοκληρώσεις task");
        }
        if (username.equals(task.getPostedBy())) {
            return error(400, "Δεν μπορείς να ολοκληρώσεις δικό σου task");
        }
        if (communityTaskRepository.complete(taskId, username, LocalDateTime.now()) == 0) {
            return error(409, "Κάποιος άλλος το ολοκλήρωσε πρώτος");
        }
        return ResponseEntity.ok(communityTaskRepository.findById(taskId).orElseThrow());
    }

    // DELETE: Μόνο αυτός που ανέβασε το task μπορεί να το σβήσει
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId, @RequestParam String username) {
        CommunityTask task = communityTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return error(404, "Το task δεν υπάρχει πια");
        }
        if (!task.getPostedBy().equals(username)) {
            return error(403, "Μπορείς να σβήσεις μόνο δικά σου tasks");
        }
        communityTaskRepository.delete(task);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST: Ο χρήστης είδε ποια από τα tasks του ολοκληρώθηκαν
    @PostMapping("/completions-seen/{username}")
    @Transactional
    public Map<String, Object> markCompletionsSeen(@PathVariable String username) {
        return Map.of("updated", communityTaskRepository.markCompletionsSeen(username));
    }

    private String cleanCategory(String category) {
        if (category == null || category.isBlank()) return null;
        String trimmed = category.trim();
        return trimmed.length() > MAX_CATEGORY_LENGTH ? trimmed.substring(0, MAX_CATEGORY_LENGTH) : trimmed;
    }

    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
