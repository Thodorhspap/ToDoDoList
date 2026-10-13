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
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Set<String> PRIORITIES = Set.of("HIGH", "MEDIUM", "LOW");
    private static final int MAX_CATEGORY_LENGTH = 30;
    private static final int MAX_SUBTASKS = 30;
    private static final int MAX_SUBTASK_LENGTH = 200;
    // Πόσες μέρες βλέπει ο αποστολέας τα tasks που ολοκλήρωσαν οι φίλοι του
    private static final int SENT_COMPLETED_DAYS = 7;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private SubtaskRepository subtaskRepository;

    // GET: Φέρνει ΜΟΝΟ τα tasks του συγκεκριμένου χρήστη (ανοιχτά και ολοκληρωμένα)
    @GetMapping("/{userId}")
    public List<Task> getTasks(@PathVariable String userId) {
        return taskRepository.findByUserId(userId);
    }

    // POST: Αποθηκεύει το νέο task και το συνδέει με τον χρήστη που το έφτιαξε
    @PostMapping("/{userId}")
    public Task createTask(@PathVariable String userId, @RequestBody Task task) {
        task.setUserId(userId); // "Κλειδώνουμε" το task πάνω στο ID του χρήστη
        task.setSentBy(null);
        task.setSeen(null);
        prepareNewTask(task);
        return taskRepository.save(task);
    }

    // POST: Ο χρήστης είδε το board του, οπότε τα νέα tasks δεν είναι πια "νέα"
    @PostMapping("/seen/{userId}")
    @Transactional
    public Map<String, Object> markSeen(@PathVariable String userId) {
        return Map.of("updated", taskRepository.markSeen(userId));
    }

    // POST: Ο χρήστης είδε ποια tasks που έστειλε ολοκληρώθηκαν
    @PostMapping("/completions-seen/{username}")
    @Transactional
    public Map<String, Object> markCompletionsSeen(@PathVariable String username) {
        return Map.of("updated", taskRepository.markCompletionsSeen(username));
    }

    // GET: Τα tasks που έστειλε ο χρήστης: τα ανοιχτά και όσα ολοκληρώθηκαν τις τελευταίες μέρες
    @GetMapping("/sent/{username}")
    public List<Task> getSentTasks(@PathVariable String username) {
        LocalDateTime since = LocalDateTime.now().minusDays(SENT_COMPLETED_DAYS);
        return taskRepository.findBySentByOrderByIdDesc(username).stream()
                .filter(t -> !Boolean.TRUE.equals(t.getDone())
                        || (t.getCompletedAt() != null && t.getCompletedAt().isAfter(since)))
                .toList();
    }

    // POST: Στέλνει ένα task στο board κάποιου άλλου χρήστη
    @PostMapping("/send/{recipient}")
    public ResponseEntity<?> sendTask(@PathVariable String recipient, @RequestBody Task task) {
        recipient = recipient.trim();
        if (task.getSentBy() == null || task.getSentBy().isBlank()) {
            return error(400, "Λείπει ο αποστολέας");
        }
        if (!userRepository.existsByUsername(recipient)) {
            return error(404, "Ο χρήστης \"" + recipient + "\" δεν υπάρχει");
        }
        if (recipient.equals(task.getSentBy())) {
            return error(400, "Για δικά σου tasks χρησιμοποίησε το board σου");
        }
        // Tasks στέλνονται μόνο σε φίλους
        boolean areFriends = friendshipRepository.findBetween(task.getSentBy(), recipient).stream()
                .anyMatch(f -> Friendship.ACCEPTED.equals(f.getStatus()));
        if (!areFriends) {
            return error(403, "Μπορείς να στέλνεις tasks μόνο σε φίλους");
        }

        task.setId(null);
        task.setUserId(recipient);
        task.setSeen(false); // για να εμφανιστεί ειδοποίηση στον παραλήπτη
        prepareNewTask(task);
        return ResponseEntity.ok(taskRepository.save(task));
    }

    // DELETE: Διαγράφει οριστικά ένα task
    @DeleteMapping("/{taskId}")
    public void deleteTask(@PathVariable Long taskId) {
        taskRepository.deleteById(taskId);
    }

    // PUT: Επεξεργασία task (τίτλος, περιγραφή, ημερομηνία, προτεραιότητα, κατηγορία)
    @PutMapping("/{taskId}")
    public ResponseEntity<?> editTask(@PathVariable Long taskId, @RequestBody Task changes) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return error(404, "Το task δεν βρέθηκε");
        }
        if (changes.getText() == null || changes.getText().isBlank()) {
            return error(400, "Ο τίτλος δεν μπορεί να είναι κενός");
        }

        task.setText(changes.getText().trim());
        task.setDescription(changes.getDescription());
        task.setDueDate(changes.getDueDate());
        task.setPriority(cleanPriority(changes.getPriority()));
        task.setCategory(cleanCategory(changes.getCategory()));
        return ResponseEntity.ok(taskRepository.save(task));
    }

    // PUT: Ολοκλήρωση ή επαναφορά task. Αν το ολοκληρώσει ο παραλήπτης, ειδοποιείται ο αποστολέας.
    @PutMapping("/{taskId}/done")
    public ResponseEntity<?> setDone(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return error(404, "Το task δεν βρέθηκε");
        }

        boolean done = Boolean.TRUE.equals(body.get("done"));
        Object username = body.get("username");
        boolean completedBySender = task.getSentBy() != null && task.getSentBy().equals(username);

        task.setDone(done);
        task.setCompletedAt(done ? LocalDateTime.now() : null);
        task.setCompletionSeen(done && task.getSentBy() != null && !completedBySender ? Boolean.FALSE : null);
        return ResponseEntity.ok(taskRepository.save(task));
    }

    // ---------------- Βήματα (checklist) ----------------
    // Όλα επιστρέφουν ολόκληρο το task, ώστε η σελίδα να ενημερώνει τη λίστα και την πρόοδο μαζί.

    // POST: Προσθέτει ένα βήμα στο τέλος της λίστας
    @PostMapping("/{taskId}/subtasks")
    @Transactional
    public ResponseEntity<?> addSubtask(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return error(404, "Το task δεν βρέθηκε");
        }
        String text = cleanSubtaskText(body.get("text"));
        if (text == null) {
            return error(400, "Το βήμα δεν μπορεί να είναι κενό");
        }
        if (task.getSubtasks().size() >= MAX_SUBTASKS) {
            return error(400, "Μέχρι " + MAX_SUBTASKS + " βήματα ανά task");
        }

        Subtask subtask = new Subtask();
        subtask.setTask(task);
        subtask.setText(text);
        subtask.setDone(false);
        subtask.setPosition(task.getSubtasks().stream().mapToInt(Subtask::getPosition).max().orElse(-1) + 1);
        task.getSubtasks().add(subtaskRepository.save(subtask));
        return ResponseEntity.ok(task);
    }

    // PUT: Τσεκάρει / ξετσεκάρει ή μετονομάζει ένα βήμα
    @PutMapping("/subtasks/{subtaskId}")
    @Transactional
    public ResponseEntity<?> editSubtask(@PathVariable Long subtaskId, @RequestBody Map<String, Object> body) {
        Subtask subtask = subtaskRepository.findById(subtaskId).orElse(null);
        if (subtask == null) {
            return error(404, "Το βήμα δεν βρέθηκε");
        }
        if (body.containsKey("text")) {
            String text = cleanSubtaskText(body.get("text"));
            if (text == null) {
                return error(400, "Το βήμα δεν μπορεί να είναι κενό");
            }
            subtask.setText(text);
        }
        if (body.containsKey("done")) {
            subtask.setDone(Boolean.TRUE.equals(body.get("done")));
        }
        // μέσα στο transaction οι αλλαγές αποθηκεύονται αυτόματα
        return ResponseEntity.ok(subtask.getTask());
    }

    // DELETE: Διαγράφει ένα βήμα
    @DeleteMapping("/subtasks/{subtaskId}")
    @Transactional
    public ResponseEntity<?> deleteSubtask(@PathVariable Long subtaskId) {
        Subtask subtask = subtaskRepository.findById(subtaskId).orElse(null);
        if (subtask == null) {
            return error(404, "Το βήμα δεν βρέθηκε");
        }
        Task task = subtask.getTask();
        task.getSubtasks().remove(subtask);   // orphanRemoval: σβήνεται από τη βάση στο τέλος του transaction
        return ResponseEntity.ok(task);
    }

    private String cleanSubtaskText(Object value) {
        if (!(value instanceof String text) || text.isBlank()) return null;
        String trimmed = text.trim();
        return trimmed.length() > MAX_SUBTASK_LENGTH ? trimmed.substring(0, MAX_SUBTASK_LENGTH) : trimmed;
    }

    // Κοινές ρυθμίσεις για κάθε νέο task
    private void prepareNewTask(Task task) {
        task.setDone(false);
        task.setCompletedAt(null);
        task.setCompletionSeen(null);
        task.setPriority(cleanPriority(task.getPriority()));
        task.setCategory(cleanCategory(task.getCategory()));
    }

    private String cleanPriority(String priority) {
        return priority != null && PRIORITIES.contains(priority) ? priority : null;
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
