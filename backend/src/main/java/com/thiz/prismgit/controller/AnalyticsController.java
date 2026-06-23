package com.thiz.prismgit.controller;

import com.thiz.prismgit.dto.ActivityDistribution;
import com.thiz.prismgit.dto.AnalyticsSummary;
import com.thiz.prismgit.dto.TimelinePoint;
import com.thiz.prismgit.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/repos/{repoId}/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Repository analytics and insights")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Get analytics summary", description = "Get summary statistics for a repository")
    @ApiResponse(responseCode = "200", description = "Analytics summary retrieved")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<AnalyticsSummary> summary(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID repoId) {
        return ResponseEntity.ok(analyticsService.summary(repoId));
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get commit timeline", description = "Get daily commit timeline for visualization")
    @ApiResponse(responseCode = "200", description = "Timeline data retrieved")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<List<TimelinePoint>> timeline(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID repoId) {
        return ResponseEntity.ok(analyticsService.timeline(repoId));
    }

    @GetMapping("/activity/hour")
    @Operation(summary = "Get hourly activity", description = "Get commit activity distribution by hour of day (0-23)")
    @ApiResponse(responseCode = "200", description = "Hourly activity data retrieved")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<List<ActivityDistribution>> activityByHour(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID repoId) {
        return ResponseEntity.ok(analyticsService.activityByHour(repoId));
    }

    @GetMapping("/activity/day")
    @Operation(summary = "Get daily activity", description = "Get commit activity distribution by day of week (0=Mon, 6=Sun)")
    @ApiResponse(responseCode = "200", description = "Daily activity data retrieved")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<List<ActivityDistribution>> activityByDay(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID repoId) {
        return ResponseEntity.ok(analyticsService.activityByDay(repoId));
    }
}
