package com.example.greetingcard.data.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UploadStatusConverterTest {

    private val converter = UploadStatusConverter()

    @Test
    fun fromStatus_pending_returnsPending() {
        assertEquals("PENDING", converter.fromStatus(UploadStatus.PENDING))
    }

    @Test
    fun fromStatus_uploading_returnsUploading() {
        assertEquals("UPLOADING", converter.fromStatus(UploadStatus.UPLOADING))
    }

    @Test
    fun fromStatus_done_returnsDone() {
        assertEquals("DONE", converter.fromStatus(UploadStatus.DONE))
    }

    @Test
    fun fromStatus_failed_returnsFailed() {
        assertEquals("FAILED", converter.fromStatus(UploadStatus.FAILED))
    }

    @Test
    fun toStatus_roundTripsAllValues() {
        for (status in UploadStatus.entries) {
            assertEquals(status, converter.toStatus(converter.fromStatus(status)))
        }
    }

    @Test
    fun toStatus_unknownString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.toStatus("UNKNOWN_STATUS")
        }
    }
}
