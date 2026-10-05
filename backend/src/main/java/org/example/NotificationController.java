package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private MessageRepository messageRepository;

    // GET: Όλες οι ειδοποιήσεις του χρήστη για τα badges
    @GetMapping("/{username}")
    public Map<String, Object> getNotifications(@PathVariable String username) {
        long newTasks = taskRepository.countByUserIdAndSeenFalse(username);
        long completedTasks = taskRepository.countBySentByAndDoneTrueAndCompletionSeenFalse(username);
        long friendRequests = friendshipRepository.countByAddresseeAndStatus(username, Friendship.PENDING);

        // Μετράμε μόνο μηνύματα από τρέχοντες φίλους (αλλιώς το badge δεν θα έσβηνε ποτέ)
        Map<String, Long> unreadByFriend = new HashMap<>();
        long unreadMessages = 0;
        for (Object[] row : messageRepository.countUnreadBySender(username)) {
            String sender = (String) row[0];
            long count = (Long) row[1];
            boolean isFriend = friendshipRepository.findBetween(username, sender).stream()
                    .anyMatch(f -> Friendship.ACCEPTED.equals(f.getStatus()));
            if (isFriend) {
                unreadByFriend.put(sender, count);
                unreadMessages += count;
            }
        }

        return Map.of(
                "newTasks", newTasks,
                "completedTasks", completedTasks,
                "friendRequests", friendRequests,
                "unreadMessages", unreadMessages,
                "unreadByFriend", unreadByFriend
        );
    }
}
