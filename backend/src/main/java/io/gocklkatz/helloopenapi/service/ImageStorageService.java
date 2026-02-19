package io.gocklkatz.helloopenapi.service;

import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

public interface ImageStorageService {
    String store(MultipartFile image, OffsetDateTime timestamp);
}
