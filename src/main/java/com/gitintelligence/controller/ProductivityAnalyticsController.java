package com.gitintelligence.controller;

import com.gitintelligence.analysis.RepositoryIntelligenceAnalyzer;
import com.gitintelligence.git.RemoteRepositoryService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class ProductivityAnalyticsController {

    private final RepositoryIntelligenceAnalyzer analyzer;
    private final RemoteRepositoryService remoteRepositoryService;

    public ProductivityAnalyticsController(
            RepositoryIntelligenceAnalyzer analyzer,
            RemoteRepositoryService remoteRepositoryService) {

        this.analyzer = analyzer;
        this.remoteRepositoryService = remoteRepositoryService;
    }

    @GetMapping("/api/repository/productivity")
    public Map<String, Object> getProductivityAnalytics(

            @RequestParam(
                    required = false
            )
            String repositoryPath,

            @RequestParam(
                    required = false
            )
            String repositoryUrl) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        try {

            /*
             * -------------------------------------------------
             * RESOLVE REPOSITORY
             * -------------------------------------------------
             */

            String resolvedRepositoryPath =
                    resolveRepositoryPath(
                            repositoryPath,
                            repositoryUrl
                    );

            if (resolvedRepositoryPath == null ||
                    resolvedRepositoryPath.isBlank()) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Repository path or GitHub repository URL is required."
                );

                return response;
            }

            Path path =
                    Path.of(
                            resolvedRepositoryPath
                    )
                    .toAbsolutePath()
                    .normalize();

            if (!Files.exists(path)) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Repository path does not exist: "
                                + resolvedRepositoryPath
                );

                return response;
            }

            if (!Files.isDirectory(path)) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Repository path is not a directory: "
                                + resolvedRepositoryPath
                );

                return response;
            }

            /*
             * -------------------------------------------------
             * RUN FULL INTELLIGENCE ANALYSIS
             * -------------------------------------------------
             */

            Map<String, Object> analysis =
                    analyzer.analyzeRepository(
                            resolvedRepositoryPath
                    );

            if (analysis == null) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Repository analysis returned no data."
                );

                return response;
            }

            /*
             * -------------------------------------------------
             * REPOSITORY INFORMATION
             * -------------------------------------------------
             */

            Object repositoryInfo =
                    analysis.get(
                            "repositoryInfo"
                    );

            Object gitSummary =
                    analysis.get(
                            "gitSummary"
                    );

            Object gitAnalytics =
                    analysis.get(
                            "gitAnalytics"
                    );

            Object sourceSummary =
                    analysis.get(
                            "sourceSummary"
                    );

            Object commitTimeline =
                    analysis.get(
                            "commitTimeline"
                    );

            Object contributorActivity =
                    analysis.get(
                            "contributorActivity"
                    );

            Object fileIntelligence =
                    analysis.get(
                            "fileIntelligence"
                    );

            Object hotspots =
                    analysis.get(
                            "hotspots"
                    );

            Object maintenanceCandidates =
                    analysis.get(
                            "maintenanceCandidates"
                    );

            Object repositoryHealth =
                    analysis.get(
                            "repositoryHealth"
                    );

            Object repositoryInsights =
                    analysis.get(
                            "repositoryInsights"
                    );

            /*
             * -------------------------------------------------
             * BUILD PRODUCTIVITY METRICS
             * -------------------------------------------------
             */

            Map<String, Object> metrics =
                    new LinkedHashMap<>();

            metrics.put(
                    "commitCount",
                    firstNumber(
                            gitSummary,
                            "commitCount",
                            "commits",
                            "totalCommits"
                    )
            );

            metrics.put(
                    "filesAdded",
                    firstNumber(
                            gitSummary,
                            "filesAdded",
                            "addedFiles",
                            "totalFilesAdded"
                    )
            );

            metrics.put(
                    "filesModified",
                    firstNumber(
                            gitSummary,
                            "filesModified",
                            "modifiedFiles",
                            "totalFilesModified"
                    )
            );

            metrics.put(
                    "filesDeleted",
                    firstNumber(
                            gitSummary,
                            "filesDeleted",
                            "deletedFiles",
                            "totalFilesDeleted"
                    )
            );

            metrics.put(
                    "filesRenamed",
                    firstNumber(
                            gitSummary,
                            "filesRenamed",
                            "renamedFiles",
                            "totalFilesRenamed"
                    )
            );

            metrics.put(
                    "linesAdded",
                    firstNumber(
                            gitSummary,
                            "linesAdded",
                            "totalLinesAdded"
                    )
            );

            metrics.put(
                    "linesRemoved",
                    firstNumber(
                            gitSummary,
                            "linesRemoved",
                            "totalLinesRemoved"
                    )
            );

            metrics.put(
                    "sourceFiles",
                    firstNumber(
                            sourceSummary,
                            "sourceFiles",
                            "sourceFileCount",
                            "totalSourceFiles"
                    )
            );

            metrics.put(
                    "sourceLines",
                    firstNumber(
                            sourceSummary,
                            "lines",
                            "totalLines",
                            "sourceLines",
                            "totalSourceLines"
                    )
            );

            metrics.put(
                    "functions",
                    firstNumber(
                            sourceSummary,
                            "functions",
                            "totalFunctions"
                    )
            );

            metrics.put(
                    "complexity",
                    firstNumber(
                            sourceSummary,
                            "complexity",
                            "totalComplexity"
                    )
            );

            metrics.put(
                    "contributors",
                    countCollection(
                            contributorActivity
                    )
            );

            metrics.put(
                    "hotspots",
                    countCollection(
                            hotspots
                    )
            );

            metrics.put(
                    "maintenanceCandidates",
                    countCollection(
                            maintenanceCandidates
                    )
            );

            /*
             * -------------------------------------------------
             * FALLBACK VALUES FROM REPOSITORY INSIGHTS
             * -------------------------------------------------
             *
             * Some values may be exposed by repositoryInsights
             * instead of sourceSummary/gitSummary.
             */

            replaceZeroWith(
                    metrics,
                    "commitCount",
                    firstNumber(
                            repositoryInsights,
                            "commitCount",
                            "commits"
                    )
            );

            replaceZeroWith(
                    metrics,
                    "sourceFiles",
                    firstNumber(
                            repositoryInsights,
                            "sourceFileCount",
                            "sourceFiles"
                    )
            );

            replaceZeroWith(
                    metrics,
                    "sourceLines",
                    firstNumber(
                            repositoryInsights,
                            "totalSourceLines",
                            "sourceLines",
                            "totalLines"
                    )
            );

            replaceZeroWith(
                    metrics,
                    "functions",
                    firstNumber(
                            repositoryInsights,
                            "totalFunctions",
                            "functions"
                    )
            );

            replaceZeroWith(
                    metrics,
                    "complexity",
                    firstNumber(
                            repositoryInsights,
                            "totalComplexity",
                            "complexity"
                    )
            );

            replaceZeroWith(
                    metrics,
                    "linesAdded",
                    firstNumber(
                            repositoryInsights,
                            "linesAdded",
                            "totalLinesAdded"
                    )
            );

            replaceZeroWith(
                    metrics,
                    "linesRemoved",
                    firstNumber(
                            repositoryInsights,
                            "linesRemoved",
                            "totalLinesRemoved"
                    )
            );

            /*
             * -------------------------------------------------
             * RETURN COMPLETE PRODUCTIVITY RESPONSE
             * -------------------------------------------------
             */

            response.put(
                    "success",
                    true
            );

            response.put(
                    "repositoryPath",
                    resolvedRepositoryPath
            );

            if (repositoryUrl != null &&
                    !repositoryUrl.isBlank()) {

                response.put(
                        "repositoryUrl",
                        repositoryUrl
                );

                response.put(
                        "remoteRepository",
                        true
                );

            } else {

                response.put(
                        "remoteRepository",
                        false
                );
            }

            response.put(
                    "repositoryInfo",
                    repositoryInfo
            );

            response.put(
                    "metrics",
                    metrics
            );

            /*
             * -------------------------------------------------
             * ANALYTICS ARRAYS
             * -------------------------------------------------
             */

            response.put(
                    "commitTimeline",
                    safeList(
                            commitTimeline
                    )
            );

            /*
             * If commitTimeline was not directly exposed,
             * try Git Analytics.
             */

            if (isEmptyCollection(
                    response.get(
                            "commitTimeline"
                    ))) {

                Object timelineFromAnalytics =
                        getValue(
                                gitAnalytics,
                                "commitTimeline"
                        );

                if (timelineFromAnalytics != null) {

                    response.put(
                            "commitTimeline",
                            safeList(
                                    timelineFromAnalytics
                            )
                    );
                }
            }

            response.put(
                    "contributorActivity",
                    safeList(
                            contributorActivity
                    )
            );

            /*
             * Same fallback for contributor activity.
             */

            if (isEmptyCollection(
                    response.get(
                            "contributorActivity"
                    ))) {

                Object contributorsFromAnalytics =
                        getValue(
                                gitAnalytics,
                                "contributorActivity"
                        );

                if (contributorsFromAnalytics != null) {

                    response.put(
                            "contributorActivity",
                            safeList(
                                    contributorsFromAnalytics
                            )
                    );
                }
            }

            response.put(
                    "fileIntelligence",
                    safeList(
                            fileIntelligence
                    )
            );

            response.put(
                    "hotspots",
                    safeList(
                            hotspots
                    )
            );

            response.put(
                    "maintenanceCandidates",
                    safeList(
                            maintenanceCandidates
                    )
            );

            /*
             * -------------------------------------------------
             * HEALTH / INSIGHTS
             * -------------------------------------------------
             */

            response.put(
                    "repositoryHealth",
                    repositoryHealth
            );

            response.put(
                    "repositoryInsights",
                    repositoryInsights
            );

            response.put(
                    "recommendations",
                    safeList(
                            analysis.get(
                                    "recommendations"
                            )
                    )
            );

            return response;

        } catch (Exception e) {

            response.clear();

            response.put(
                    "success",
                    false
            );

            if (repositoryUrl != null &&
                    !repositoryUrl.isBlank()) {

                response.put(
                        "repositoryUrl",
                        repositoryUrl
                );

                response.put(
                        "remoteRepository",
                        true
                );
            }

            response.put(
                    "message",
                    "Unable to calculate productivity analytics: "
                            + e.getMessage()
            );

            return response;
        }
    }

    /*
     * =========================================================
     * REPOSITORY RESOLUTION
     * =========================================================
     */

    private String resolveRepositoryPath(
            String repositoryPath,
            String repositoryUrl) throws Exception {

        /*
         * GitHub URL takes priority when supplied.
         */
        if (repositoryUrl != null &&
                !repositoryUrl.isBlank()) {

            Path remotePath =
                    remoteRepositoryService
                            .getOrCloneRepository(
                                    repositoryUrl
                            );

            return remotePath.toString();
        }

        /*
         * Otherwise use local repository path.
         */
        if (repositoryPath != null &&
                !repositoryPath.isBlank()) {

            return repositoryPath;
        }

        return null;
    }

    /*
     * =========================================================
     * GENERIC MAP VALUE HELPERS
     * =========================================================
     */

    private Object getValue(
            Object source,
            String key) {

        if (source instanceof Map<?, ?> map) {

            return map.get(key);
        }

        return null;
    }

    private long firstNumber(
            Object source,
            String... keys) {

        if (!(source instanceof Map<?, ?> map)) {
            return 0L;
        }

        for (String key : keys) {

            Object value =
                    map.get(key);

            Long number =
                    toLong(value);

            if (number != null) {
                return number;
            }
        }

        return 0L;
    }

    private Long toLong(
            Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text) {

            try {

                return Long.parseLong(
                        text.trim()
                );

            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    /*
     * =========================================================
     * COLLECTION HELPERS
     * =========================================================
     */

    private long countCollection(
            Object value) {

        if (value == null) {
            return 0L;
        }

        if (value instanceof List<?> list) {
            return list.size();
        }

        if (value instanceof java.util.Collection<?> collection) {
            return collection.size();
        }

        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(
                    value
            );
        }

        return 0L;
    }

    private List<?> safeList(
            Object value) {

        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof List<?> list) {
            return list;
        }

        if (value instanceof java.util.Collection<?> collection) {
            return new ArrayList<>(
                    collection
            );
        }

        return new ArrayList<>();
    }

    private boolean isEmptyCollection(
            Object value) {

        return countCollection(
                value
        ) == 0;
    }

    /*
     * =========================================================
     * FALLBACK METRIC HELPER
     * =========================================================
     */

    private void replaceZeroWith(
            Map<String, Object> metrics,
            String metricName,
            long fallbackValue) {

        if (fallbackValue <= 0) {
            return;
        }

        Object current =
                metrics.get(
                        metricName
                );

        Long currentNumber =
                toLong(current);

        if (currentNumber == null ||
                currentNumber == 0L) {

            metrics.put(
                    metricName,
                    fallbackValue
            );
        }
    }
}