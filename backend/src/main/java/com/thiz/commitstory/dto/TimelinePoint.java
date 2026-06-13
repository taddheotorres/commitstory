package com.thiz.commitstory.dto;

import java.time.LocalDate;

public record TimelinePoint(
        LocalDate date,
        long commitCount
) {}
