package com.logplatform.ingestion;

import com.logplatform.ingestion.dto.LogEntryDto;
import com.logplatform.ingestion.kafka.LogEventProducer;
import com.logplatform.ingestion.model.LogEntry;
import com.logplatform.ingestion.repository.LogEntryRepository;
import com.logplatform.ingestion.service.LogIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogIngestionServiceTest {

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private LogEventProducer logEventProducer;

    @InjectMocks
    private LogIngestionService logIngestionService;

    @Test
    void shouldIngestSingleLog() {
        LogEntryDto dto = LogEntryDto.builder()
                .serviceName("payment-service")
                .logLevel("ERROR")
                .message("Payment gateway timeout")
                .timestamp(Instant.now())
                .traceId("trace-123")
                .environment("prod")
                .build();

        LogEntry mockSaved = LogEntry.builder()
                .id("uuid-1")
                .serviceName("payment-service")
                .logLevel("ERROR")
                .message("Payment gateway timeout")
                .timestamp(dto.getTimestamp())
                .source("REST")
                .ingestedAt(Instant.now())
                .build();

        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(mockSaved);

        LogEntry result = logIngestionService.ingestLog(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("uuid-1");
        assertThat(result.getServiceName()).isEqualTo("payment-service");
        assertThat(result.getLogLevel()).isEqualTo("ERROR");

        verify(logEntryRepository, times(1)).save(any(LogEntry.class));
        verify(logEventProducer, times(1)).publishLogEntry(any(LogEntry.class));
    }
}
