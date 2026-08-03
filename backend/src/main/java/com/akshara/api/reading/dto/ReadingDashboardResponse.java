package com.akshara.api.reading.dto;

import java.util.List;

public record ReadingDashboardResponse(
        ReadingStatisticsResponse statistics,
        ReadingGoalResponse goal,
        List<ReadingEntryResponse> entries
) {
}
