package io.gocklkatz.helloopenapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.image-storage.backend", havingValue = "s3")
public class ImageStorageServiceS3 implements ImageStorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public ImageStorageServiceS3(S3Client s3Client,
                                 @Value("${app.image-storage.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public String store(MultipartFile image, OffsetDateTime timestamp) {
        String year = String.format("%04d", timestamp.getYear());
        String month = String.format("%02d", timestamp.getMonthValue());
        String day = String.format("%02d", timestamp.getDayOfMonth());

        String originalFilename = image.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : ".jpg";
        String key = year + "/" + month + "/" + day + "/reading_" + UUID.randomUUID() + ext;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(image.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(image.getBytes()));
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to upload image to S3", e);
        } catch (SdkException e) {
            throw new RuntimeException("Failed to upload image to S3", e);
        }
    }
}
