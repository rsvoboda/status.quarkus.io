package io.quarkus.status.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RegisterForReflection
public class StatsPerArea {

    public String name;
    public String kindLabel;
    public LocalDateTime updated;

    public List<String> chartLabels = new LinkedList<>();;
    public Map<String, DatasetDetails> perAreaMap;
    public Map<String, String> colorsMap;

    public static class DatasetDetails {
        public String color;
        public List<StatsEntry> statsEntries;

        public DatasetDetails(String color, List<StatsEntry> statsEntries) {
            this.color = color;
            this.statsEntries = statsEntries;
        }
    }
}
