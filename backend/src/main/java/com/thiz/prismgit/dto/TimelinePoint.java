package com.thiz.prismgit.dto;

import java.time.LocalDate;

public record TimelinePoint(
        LocalDate date,
        long commitCount
) {}
