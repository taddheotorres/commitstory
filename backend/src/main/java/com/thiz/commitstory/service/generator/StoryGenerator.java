package com.thiz.commitstory.service.generator;

import com.thiz.commitstory.entity.CommitEntry;

import java.util.List;
import java.util.Map;

public interface StoryGenerator {

    String generate(List<CommitEntry> commits, Map<String, String> options);
}
