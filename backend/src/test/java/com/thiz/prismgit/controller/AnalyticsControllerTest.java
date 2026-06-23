package com.thiz.prismgit.controller;

import com.thiz.prismgit.dto.ActivityDistribution;
import com.thiz.prismgit.dto.AnalyticsSummary;
import com.thiz.prismgit.dto.TimelinePoint;
import com.thiz.prismgit.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    private UUID repoId;

    @BeforeEach
    void setUp() {
        repoId = UUID.randomUUID();
    }

    @Test
    void should_get_analytics_summary() throws Exception {
        AnalyticsSummary summary = new AnalyticsSummary(
                10, 3, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), 5,
                List.of(), List.of()
        );
        when(analyticsService.summary(repoId)).thenReturn(summary);

        mockMvc.perform(get("/api/repos/{repoId}/analytics/summary", repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCommits").value(10))
                .andExpect(jsonPath("$.totalAuthors").value(3))
                .andExpect(jsonPath("$.totalFilesChanged").value(5));

        verify(analyticsService).summary(repoId);
    }

    @Test
    void should_get_timeline() throws Exception {
        TimelinePoint point1 = new TimelinePoint(LocalDate.of(2024, 1, 1), 5);
        TimelinePoint point2 = new TimelinePoint(LocalDate.of(2024, 1, 2), 3);
        when(analyticsService.timeline(repoId)).thenReturn(List.of(point1, point2));

        mockMvc.perform(get("/api/repos/{repoId}/analytics/timeline", repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2024-01-01"))
                .andExpect(jsonPath("$[0].commitCount").value(5))
                .andExpect(jsonPath("$[1].date").value("2024-01-02"))
                .andExpect(jsonPath("$[1].commitCount").value(3))
                .andExpect(jsonPath("$.length()").value(2));

        verify(analyticsService).timeline(repoId);
    }

    @Test
    void should_get_activity_by_hour() throws Exception {
        List<ActivityDistribution> activity = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            activity.add(new ActivityDistribution(i, i == 10 ? 5 : 0));
        }
        when(analyticsService.activityByHour(repoId)).thenReturn(activity);

        mockMvc.perform(get("/api/repos/{repoId}/analytics/activity/hour", repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(24))
                .andExpect(jsonPath("$[10].commitCount").value(5))
                .andExpect(jsonPath("$[0].commitCount").value(0));

        verify(analyticsService).activityByHour(repoId);
    }

    @Test
    void should_get_activity_by_day() throws Exception {
        List<ActivityDistribution> activity = List.of(
                new ActivityDistribution(0, 8),  // Monday
                new ActivityDistribution(1, 10), // Tuesday
                new ActivityDistribution(2, 7),  // Wednesday
                new ActivityDistribution(3, 6),  // Thursday
                new ActivityDistribution(4, 9),  // Friday
                new ActivityDistribution(5, 4),  // Saturday
                new ActivityDistribution(6, 3)   // Sunday
        );
        when(analyticsService.activityByDay(repoId)).thenReturn(activity);

        mockMvc.perform(get("/api/repos/{repoId}/analytics/activity/day", repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].commitCount").value(8))
                .andExpect(jsonPath("$[1].commitCount").value(10))
                .andExpect(jsonPath("$[6].commitCount").value(3));

        verify(analyticsService).activityByDay(repoId);
    }

    @Test
    void should_return_empty_timeline_when_no_commits() throws Exception {
        when(analyticsService.timeline(repoId)).thenReturn(List.of());

        mockMvc.perform(get("/api/repos/{repoId}/analytics/timeline", repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(analyticsService).timeline(repoId);
    }
}
