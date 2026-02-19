package io.gocklkatz.helloopenapi.repository;

import com.example.model.Reading;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class ReadingRepositoryImpl implements ReadingRepository {

    private final Map<Integer, Reading> store = new HashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    @Override
    public Reading save(Reading reading) {
        int id = idSequence.incrementAndGet();
        Reading saved = new Reading(id, reading.getTimestamp(), reading.getImagePath());
        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Reading> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Reading> findAll() {
        return new ArrayList<>(store.values());
    }
}
