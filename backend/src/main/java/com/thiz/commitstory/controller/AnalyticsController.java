package com.thiz.commitstory.controller;

import com.thiz.commitstory.dto.ActivityDistribution;
import com.thiz.commitstory.dto.AnalyticsSummary;
import com.thiz.commitstory.dto.TimelinePoint;
import com.thiz.commitstory.service.AnalyticsService;
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
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummary> summary(@PathVariable UUID repoId) {
        return ResponseEntity.ok(analyticsService.summary(repoId));
    }

    @GetMapping("/timeline")
    public ResponseEntity<List<TimelinePoint>> timeline(@PathVariable UUID repoId) {
        return ResponseEntity.ok(analyticsService.timeline(repoId));
    }

    @GetMapping("/activity/hour")
    public ResponseEntity<List<ActivityDistribution>> activityByHour(@PathVariable UUID repoId) {
        return ResponseEntity.ok(analyticsService.activityByHour(repoId));
    }

    @GetMapping("/activity/day")
    public ResponseEntity<List<ActivityDistribution>> activityByDay(@PathVariable UUID repoId) {
        return ResponseEntity.ok(analyticsService.activityByDay(repoId));
    }
}
