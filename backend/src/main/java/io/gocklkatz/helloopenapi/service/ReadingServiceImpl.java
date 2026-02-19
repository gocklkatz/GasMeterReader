package io.gocklkatz.helloopenapi.service;

import com.example.model.Reading;
import io.gocklkatz.helloopenapi.repository.ReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReadingServiceImpl implements ReadingService {

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ImageStorageService imageStorageService;
    private final ReadingRepository readingRepository;

    public ReadingServiceImpl(ImageStorageService imageStorageService, ReadingRepository readingRepository) {
        this.imageStorageService = imageStorageService;
        this.readingRepository = readingRepository;
    }

    @Override
    public Reading createReading(MultipartFile image, OffsetDateTime timestamp) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image type: " + contentType);
        }
        String imagePath = imageStorageService.store(image, timestamp);
        Reading reading = new Reading();
        reading.setTimestamp(timestamp);
        reading.setImagePath(imagePath);
        return readingRepository.save(reading);
    }

    @Override
    public List<Reading> getAllReadings() {
        return readingRepository.findAll();
    }

    @Override
    public Optional<Reading> getReadingById(Integer id) {
        return readingRepository.findById(id);
    }
}
