package com.sentstorm.messenger.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response wrapper")
public class PageResponse<T> {

    @Schema(description = "List of items for the current page")
    private List<T> items;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "20")
    private int size;

    @Schema(description = "Total number of items across all pages", example = "42")
    private long total;

    @Schema(description = "Whether there are more pages available", example = "true")
    private boolean hasNext;
}