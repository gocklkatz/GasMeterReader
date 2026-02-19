package io.gocklkatz.helloopenapi.service;

import com.example.model.Reading;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReadingService {
    Reading createReading(MultipartFile image, OffsetDateTime timestamp);
    List<Reading> getAllReadings();
    Optional<Reading> getReadingById(Integer id);
}
