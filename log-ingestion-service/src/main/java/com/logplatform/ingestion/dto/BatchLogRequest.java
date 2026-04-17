package com.logplatform.ingestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchLogRequest {

    @NotEmpty(message = "logs list cannot be empty")
    @Size(max = 1000, message = "Maximum 1000 logs per batch")
    @Valid
    private List<LogEntryDto> logs;
}
