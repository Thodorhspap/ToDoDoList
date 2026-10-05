package org.example;

import jakarta.persistence.*;

@Entity
@Table(name = "counters")
public class Counter {

    @Id
    private String name;

    @Column(name = "count_value", nullable = false)
    private Long value = 0L;

    public Counter() {}

    public Counter(String name, Long value) {
        this.name = name;
        this.value = value;
    }

    // --- Getters και Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
}
