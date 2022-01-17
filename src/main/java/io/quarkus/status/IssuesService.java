package io.quarkus.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.status.github.GitHubService;
import io.quarkus.status.model.ColorNames;
import io.quarkus.status.model.Label;
import io.quarkus.status.model.Stats;
import io.quarkus.status.model.StatsEntry;
import io.quarkus.status.model.StatsPerArea;
import io.quarkus.status.model.StringTuple;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class IssuesService {

    private static final String QUARKUS_REPOSITORY = "quarkusio/quarkus";
    private static final String BUG_NAME = "Bugs";
    public static final String BUG_LABEL = "kind/bug";
    private static final String ENHANCEMENT_NAME = "Enhancements";
    public static final String ENHANCEMENT_LABEL = "kind/enhancement";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Inject
    GitHubService gitHubService;

    @Inject
    LabelsService labelsService;

    @ConfigProperty(name = "status.issues.stats.start", defaultValue = "2019-01-01")
    LocalDate issuesStatsStart;

    private volatile Stats bugsStats;
    private volatile Stats enhancementsStats;

    @Scheduled(every = "6H")
    public void updateStatus() throws Exception {
        bugsStats = buildIssuesMonthlyStats(BUG_NAME, BUG_LABEL);
        enhancementsStats = buildIssuesMonthlyStats(ENHANCEMENT_NAME, ENHANCEMENT_LABEL);
    }

    public Stats getBugsMonthlyStats() throws Exception {
        Stats localStats = bugsStats;
        if (localStats == null) {
            synchronized (this) {
                localStats = bugsStats;
                if (localStats == null) {
                    bugsStats = localStats = buildIssuesMonthlyStats(BUG_NAME, BUG_LABEL);
                }
            }
        }
        return localStats;
    }

    public Stats getEnhancementsMonthlyStats() throws Exception {
        Stats localStats = enhancementsStats;
        if (localStats == null) {
            synchronized (this) {
                localStats = enhancementsStats;
                if (localStats == null) {
                    enhancementsStats = localStats = buildIssuesMonthlyStats(ENHANCEMENT_NAME, ENHANCEMENT_LABEL);
                }
            }
        }
        return localStats;
    }

    public StatsPerArea getIssuesPerArea() throws Exception {
        LocalDate start = issuesStatsStart;
        LocalDate stopTime = LocalDate.now().withDayOfMonth(2);

        List<StringTuple> areaLabelTuples =
                labelsService.getBugsLabels().stream()
                        .filter(label -> label.name.startsWith("area/"))
                        .sorted(Comparator.comparing(label -> label.name))
                        .map(this::convertLabelToTuple)
                        .collect(Collectors.toList());

//        System.out.println("labelsService.getBugsLabels() " + labelsService.getBugsLabels());
//        System.out.println("areaLabelTuples " + areaLabelTuples);

        Map<String, StatsPerArea.DatasetDetails> perAreaMap = new TreeMap<>();
        Map<String, String> colorsMap = new TreeMap<>();
        Iterator<String> colors = ColorNames.chartColors().iterator();
        for (StringTuple keyTuple : areaLabelTuples) {
            List<StatsEntry> entries = new LinkedList<>();
            perAreaMap.put(keyTuple.y, new StatsPerArea.DatasetDetails(colors.next(), entries));
            colorsMap.put(keyTuple.y, "#00CC00");
        }

        StatsPerArea statsPerArea = new StatsPerArea();
        statsPerArea.name = BUG_NAME;
        statsPerArea.kindLabel = BUG_LABEL;
        statsPerArea.updated = LocalDateTime.now();
        statsPerArea.colorsMap = colorsMap;

        while (start.isBefore(stopTime)) {
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            String timeWindow = start + ".." + end;
            String chartLabel = FORMATTER.format(start);
            statsPerArea.chartLabels.add(chartLabel);

            List<StatsEntry> entriesInMonth = gitHubService.issuesPerArea(QUARKUS_REPOSITORY, BUG_LABEL, areaLabelTuples, timeWindow);
            entriesInMonth.forEach(statsEntry -> {
                String label = statsEntry.entryName;
                statsEntry.entryName = chartLabel;
                perAreaMap.get(label).statsEntries.add(statsEntry);
            });

            start = start.plusMonths(1);
        }


        statsPerArea.perAreaMap = perAreaMap;
        return statsPerArea;
    }
    
    private StringTuple convertLabelToTuple(Label label) {
        StringTuple tuple = new StringTuple(replaceInvalidGraphqlCharacters(label), label.name);
        return tuple;
    }

    private String replaceInvalidGraphqlCharacters(Label label) {
        return label.name
                .replaceAll(" ", "_")
                .replaceAll("-", "_")
                .replaceAll("/", "_");
    }

    private Stats buildIssuesMonthlyStats(String name, String label) throws Exception {
        Stats stats = new Stats();
        stats.name = name;
        stats.label = label;
        stats.updated = LocalDateTime.now();
        stats.repository = QUARKUS_REPOSITORY;

        LocalDate start = issuesStatsStart;
        LocalDate stopTime = LocalDate.now().withDayOfMonth(2);

        while (start.isBefore(stopTime)) {
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            String timeWindow = start + ".." + end;

            StatsEntry statsEntry = gitHubService.issuesStats(QUARKUS_REPOSITORY, label, timeWindow, FORMATTER.format(start));
            stats.add(statsEntry);

            start = start.plusMonths(1);
        }

        return stats;
    }

}