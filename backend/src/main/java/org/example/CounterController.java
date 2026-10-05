package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/counter")
public class CounterController {

    private static final String GLOBAL = "global";
    private static final long START_VALUE = 1337L;

    @Autowired
    private CounterRepository counterRepository;

    // GET: Επιστρέφει την τρέχουσα τιμή του Global Clicker
    @GetMapping
    @Transactional
    public Map<String, Long> getCount() {
        return Map.of("count", getOrCreate().getValue());
    }

    // POST: Αυξάνει τον Global Clicker κατά 1 και επιστρέφει τη νέα τιμή
    @PostMapping("/increment")
    @Transactional
    public Map<String, Long> increment() {
        getOrCreate();
        counterRepository.increment(GLOBAL);
        Counter counter = counterRepository.findById(GLOBAL).orElseThrow();
        return Map.of("count", counter.getValue());
    }

    private Counter getOrCreate() {
        return counterRepository.findById(GLOBAL)
                .orElseGet(() -> counterRepository.saveAndFlush(new Counter(GLOBAL, START_VALUE)));
    }
}
