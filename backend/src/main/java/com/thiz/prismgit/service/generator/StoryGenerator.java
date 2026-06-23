package com.thiz.prismgit.service.generator;

import com.thiz.prismgit.entity.CommitEntry;

import java.util.List;
import java.util.Map;

public interface StoryGenerator {

    String generate(List<CommitEntry> commits, Map<String, String> options);
}
