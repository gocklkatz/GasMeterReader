package io.gocklkatz.helloopenapi.repository;

import com.example.model.Reading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReadingRepositoryImplTest {

    private ReadingRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ReadingRepositoryImpl();
    }

    @Test
    void save_newReading_assignsIdAndReturnsIt() {
        Reading reading = new Reading();
        reading.setTimestamp(OffsetDateTime.parse("2026-02-19T08:00:00Z"));
        reading.setImagePath("2026/02/19/reading_abc.jpg");

        Reading saved = repository.save(reading);

        assertThat(saved.getId()).isEqualTo(1);
        assertThat(saved.getTimestamp()).isEqualTo(OffsetDateTime.parse("2026-02-19T08:00:00Z"));
        assertThat(saved.getImagePath()).isEqualTo("2026/02/19/reading_abc.jpg");
    }

    @Test
    void save_multipleReadings_assignsSequentialIds() {
        Reading r1 = new Reading();
        r1.setTimestamp(OffsetDateTime.parse("2026-02-19T08:00:00Z"));
        r1.setImagePath("2026/02/19/reading_a.jpg");

        Reading r2 = new Reading();
        r2.setTimestamp(OffsetDateTime.parse("2026-02-20T08:00:00Z"));
        r2.setImagePath("2026/02/20/reading_b.jpg");

        Reading saved1 = repository.save(r1);
        Reading saved2 = repository.save(r2);

        assertThat(saved1.getId()).isEqualTo(1);
        assertThat(saved2.getId()).isEqualTo(2);
    }

    @Test
    void findById_savedReading_returnsIt() {
        Reading reading = new Reading();
        reading.setTimestamp(OffsetDateTime.parse("2026-02-19T08:00:00Z"));
        reading.setImagePath("2026/02/19/reading_abc.jpg");
        Reading saved = repository.save(reading);

        Optional<Reading> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        Optional<Reading> result = repository.findById(99);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_emptyRepository_returnsEmptyList() {
        List<Reading> all = repository.findAll();

        assertThat(all).isEmpty();
    }

    @Test
    void findAll_returnsAllSavedReadings() {
        Reading r1 = new Reading();
        r1.setTimestamp(OffsetDateTime.parse("2026-02-19T08:00:00Z"));
        r1.setImagePath("path1.jpg");

        Reading r2 = new Reading();
        r2.setTimestamp(OffsetDateTime.parse("2026-02-20T08:00:00Z"));
        r2.setImagePath("path2.jpg");

        repository.save(r1);
        repository.save(r2);

        List<Reading> all = repository.findAll();

        assertThat(all).hasSize(2);
        assertThat(all).extracting(Reading::getImagePath).containsExactlyInAnyOrder("path1.jpg", "path2.jpg");
    }

    @Test
    void save_doesNotModifyInputReading() {
        Reading reading = new Reading();
        reading.setTimestamp(OffsetDateTime.parse("2026-02-19T08:00:00Z"));
        reading.setImagePath("2026/02/19/reading_abc.jpg");

        repository.save(reading);

        assertThat(reading.getId()).isNull();
    }

    @Test
    void findById_withMultipleReadings_returnsCorrectOne() {
        Reading r1 = new Reading();
        r1.setTimestamp(OffsetDateTime.parse("2026-02-19T08:00:00Z"));
        r1.setImagePath("path1.jpg");

        Reading r2 = new Reading();
        r2.setTimestamp(OffsetDateTime.parse("2026-02-20T08:00:00Z"));
        r2.setImagePath("path2.jpg");

        Reading saved1 = repository.save(r1);
        repository.save(r2);

        Optional<Reading> result = repository.findById(saved1.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getImagePath()).isEqualTo("path1.jpg");
    }
}
