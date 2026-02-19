package io.gocklkatz.helloopenapi.controller;

import com.example.api.ReadingsApi;
import com.example.model.Reading;
import io.gocklkatz.helloopenapi.service.ReadingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
public class ReadingController implements ReadingsApi {

    private final ReadingService readingService;

    public ReadingController(ReadingService readingService) {
        this.readingService = readingService;
    }

    @Override
    public ResponseEntity<Reading> getReadingById(Integer id) {
        return readingService.getReadingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<Reading>> getAllReadings() {
        return ResponseEntity.ok(readingService.getAllReadings());
    }

    @Override
    public ResponseEntity<Reading> createReading(MultipartFile image, OffsetDateTime timestamp) {
        Reading reading = readingService.createReading(image, timestamp);
        return ResponseEntity.status(HttpStatus.CREATED).body(reading);
    }
}
