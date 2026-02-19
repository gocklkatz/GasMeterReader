package io.gocklkatz.helloopenapi.repository;

import com.example.model.Reading;

import java.util.List;
import java.util.Optional;

public interface ReadingRepository {
    Reading save(Reading reading);
    Optional<Reading> findById(Integer id);
    List<Reading> findAll();
}
